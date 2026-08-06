package net.conczin.immersive_paintings.fabric.compat;

import net.conczin.immersive_paintings.ClientPaintingManager;
import net.conczin.immersive_paintings.Main;
import net.conczin.immersive_paintings.Painting;
import net.conczin.immersive_paintings.entity.ImmersivePaintingEntity;
import net.conczin.immersive_paintings.registry.Configs;
import net.conczin.immersive_paintings.util.Cache;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional bridge to MineAstr's image translation API. Reflection keeps the
 * Fabric build independent from MineAstr and leaves NeoForge unaffected.
 */
public final class MineAstrTranslationCompat {
    private static final String MINEASTR_MOD_ID = "mineastr";
    private static final int MAX_IMAGE_BYTES = 768 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 2048;
    private static final long RETRY_DELAY_MS = 30_000L;
    private static final String CONTEXT =
            "This image is displayed by Immersive Paintings. Preserve proper nouns and line breaks.";
    private static final String PROMPT =
            "Translate only text visible in the image. Do not describe the image. Preserve line breaks and return plain text.";

    private static final ExecutorService IMAGE_ENCODER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ImmersivePaintings-MineAstrEncoder");
        thread.setDaemon(true);
        return thread;
    });

    private static final Cache<TranslationKey, Translation> TRANSLATIONS = new TranslationCache();
    private static final Map<TranslationKey, Long> RETRY_AT = new HashMap<>();
    private static final Set<TranslationKey> PENDING = new HashSet<>();

    private static Method requestImageTranslation;
    private static Method resultSourceText;
    private static Method resultTranslations;
    private static Method showEntityTranslation;
    private static Method removeTranslation;
    private static Object translationsEnabledValue;
    private static Method translationsEnabledGetter;

    private static boolean available;
    private static Object activeLevel;
    private static String activeDisplayId;

    private MineAstrTranslationCompat() {
    }

    public static void initialize() {
        if (!FabricLoader.getInstance().isModLoaded(MINEASTR_MOD_ID)) {
            return;
        }

        try {
            Class<?> clientClass = Class.forName("com.mineastr.MineAstrClient");
            Class<?> resultClass = Class.forName("com.mineastr.MineAstrPayloads$ImageTranslationResult");
            Class<?> displayClass = Class.forName("com.mineastr.api.MineAstrDisplayApi");

            requestImageTranslation = clientClass.getMethod(
                    "requestImageTranslation",
                    byte[].class,
                    String.class,
                    List.class,
                    String.class,
                    String.class);
            resultSourceText = resultClass.getMethod("sourceText");
            resultTranslations = resultClass.getMethod("translations");
            showEntityTranslation = displayClass.getMethod(
                    "showEntityTranslation",
                    String.class,
                    int.class,
                    Vec3.class,
                    String.class,
                    String.class);
            removeTranslation = displayClass.getMethod("remove", String.class);
            findTranslationsEnabledGetter();

            available = true;
            ClientTickEvents.END_CLIENT_TICK.register(MineAstrTranslationCompat::tick);
            Main.LOGGER.info("Enabled optional MineAstr painting translation integration");
        } catch (ReflectiveOperationException | LinkageError error) {
            Main.LOGGER.warn(
                    "MineAstr is installed but its image translation API is unavailable; version 0.6.21 or newer is required",
                    error);
        }
    }

    private static void findTranslationsEnabledGetter() {
        try {
            Class<?> configClass = Class.forName("com.mineastr.MineAstrClientConfig");
            translationsEnabledValue = configClass.getField("GAME_TRANSLATIONS_ENABLED").get(null);
            translationsEnabledGetter = translationsEnabledValue.getClass().getDeclaredMethod("getAsBoolean");
            translationsEnabledGetter.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException error) {
            translationsEnabledValue = null;
            translationsEnabledGetter = null;
            Main.LOGGER.debug("MineAstr translation preference could not be read; the public image API remains enabled", error);
        }
    }

    private static void tick(Minecraft client) {
        if (!available) {
            return;
        }
        if (client.level != activeLevel) {
            resetForLevel(client.level);
        }
        if (!translationsEnabled()
                || client.level == null
                || client.player == null
                || client.screen != null
                || !(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof ImmersivePaintingEntity painting)) {
            removeActiveDisplay();
            return;
        }

        Identifier motive = painting.getMotive();
        if (motive.equals(Main.NONE_LOCATION)) {
            removeActiveDisplay();
            return;
        }

        if (!Configs.CLIENT.showNSFWPaintings
                && ClientPaintingManager.getPainting(motive)
                .map(metadata -> metadata.has(Painting.Flag.NSFW))
                .orElse(false)) {
            removeActiveDisplay();
            return;
        }

        String language = normalizeLanguage(client.getLanguageManager().getSelected());
        if (language.isBlank()) {
            removeActiveDisplay();
            return;
        }

        TranslationKey key = new TranslationKey(getImageCacheKey(motive), language);
        Translation translation = TRANSLATIONS.get(key).orElse(null);
        if (translation != null) {
            if (translation.translated().isBlank()) {
                removeActiveDisplay();
            } else {
                showTranslation(painting, translation);
            }
            return;
        }

        removeActiveDisplay();
        long now = System.currentTimeMillis();
        if (!PENDING.isEmpty() || RETRY_AT.getOrDefault(key, 0L) > now) {
            return;
        }

        ClientPaintingManager.getFullImage(motive)
                .ifPresent(image -> requestTranslation(client, key, image));
    }

    private static boolean translationsEnabled() {
        if (translationsEnabledValue == null || translationsEnabledGetter == null) {
            return true;
        }
        try {
            return (boolean) translationsEnabledGetter.invoke(translationsEnabledValue);
        } catch (ReflectiveOperationException | RuntimeException error) {
            Main.LOGGER.debug("Unable to read MineAstr's game translation preference", error);
            return false;
        }
    }

    private static void requestTranslation(
            Minecraft client,
            TranslationKey key,
            BufferedImage image) {
        if (!PENDING.add(key)) {
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            try {
                return encodeForMineAstr(image);
            } catch (IOException error) {
                throw new CompletionException(error);
            }
        }, IMAGE_ENCODER).thenCompose(bytes -> invokeTranslationRequest(bytes, key.language()))
                .whenComplete((result, error) -> client.execute(() ->
                        finishRequest(key, result, error)));
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<Object> invokeTranslationRequest(byte[] imageBytes, String language) {
        try {
            Object result = requestImageTranslation.invoke(
                    null,
                    imageBytes,
                    "image/jpeg",
                    List.of(language),
                    CONTEXT,
                    PROMPT);
            if (result instanceof CompletableFuture<?> future) {
                return (CompletableFuture<Object>) future;
            }
            return CompletableFuture.failedFuture(
                    new IllegalStateException("MineAstr returned an unsupported async result"));
        } catch (IllegalAccessException error) {
            return CompletableFuture.failedFuture(error);
        } catch (InvocationTargetException error) {
            return CompletableFuture.failedFuture(
                    error.getCause() == null ? error : error.getCause());
        }
    }

    private static void finishRequest(
            TranslationKey key,
            Object result,
            Throwable error) {
        PENDING.remove(key);
        if (error != null) {
            RETRY_AT.put(key, System.currentTimeMillis() + RETRY_DELAY_MS);
            Main.LOGGER.debug("MineAstr image translation failed for {}", key.imageKey(), unwrap(error));
            return;
        }

        try {
            String source = stringValue(resultSourceText.invoke(result));
            Map<?, ?> translations = resultTranslations.invoke(result) instanceof Map<?, ?> map
                    ? map
                    : Map.of();
            String translated = selectTranslation(translations, key.language());
            if (translated.isBlank()) {
                translated = source;
            }
            TRANSLATIONS.set(key, new Translation(translated.strip(), source.strip()));
            RETRY_AT.remove(key);
        } catch (ReflectiveOperationException | RuntimeException extractionError) {
            RETRY_AT.put(key, System.currentTimeMillis() + RETRY_DELAY_MS);
            Main.LOGGER.warn("MineAstr returned an unreadable image translation result", extractionError);
        }
    }

    private static String selectTranslation(Map<?, ?> translations, String language) {
        for (Map.Entry<?, ?> entry : translations.entrySet()) {
            if (normalizeLanguage(stringValue(entry.getKey())).equals(language)) {
                String value = stringValue(entry.getValue()).strip();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }

        int separator = language.indexOf('_');
        String family = separator < 0 ? language : language.substring(0, separator);
        for (Map.Entry<?, ?> entry : translations.entrySet()) {
            String candidate = normalizeLanguage(stringValue(entry.getKey()));
            String value = stringValue(entry.getValue()).strip();
            if (!value.isBlank()
                    && (candidate.equals(family) || candidate.startsWith(family + "_"))) {
                return value;
            }
        }
        return "";
    }

    private static void showTranslation(ImmersivePaintingEntity painting, Translation translation) {
        String displayId = "immersive-painting:" + painting.getId();
        if (!displayId.equals(activeDisplayId)) {
            removeActiveDisplay();
            activeDisplayId = displayId;
        }

        try {
            showEntityTranslation.invoke(
                    null,
                    displayId,
                    painting.getId(),
                    new Vec3(0.0, painting.getBbHeight() + 0.2, 0.0),
                    translation.translated(),
                    translation.original());
        } catch (ReflectiveOperationException | RuntimeException error) {
            Main.LOGGER.warn("Failed to submit a painting translation to MineAstr's display API", error);
            removeActiveDisplay();
        }
    }

    private static void resetForLevel(Object level) {
        activeLevel = level;
        RETRY_AT.clear();
        removeActiveDisplay();
    }

    private static String getImageCacheKey(Identifier motive) {
        // A custom painting has one metadata hash plus full/thumbnail server cache
        // entries. The metadata hash is the common identity for all three.
        return ClientPaintingManager.getPainting(motive)
                .map(painting -> {
                    String hash = painting.hash();
                    if (!hash.isBlank()) {
                        return painting.type().getSerializedName().toLowerCase(Locale.ROOT) + ":" + hash;
                    }
                    return motive.toString();
                })
                .orElse(motive.toString());
    }

    private static void removeActiveDisplay() {
        String displayId = activeDisplayId;
        activeDisplayId = null;
        if (displayId == null || removeTranslation == null) {
            return;
        }
        try {
            removeTranslation.invoke(null, displayId);
        } catch (ReflectiveOperationException | RuntimeException error) {
            Main.LOGGER.debug("Failed to remove a MineAstr painting translation display", error);
        }
    }

    private static byte[] encodeForMineAstr(BufferedImage source) throws IOException {
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            throw new IOException("Painting image is empty");
        }

        float initialScale = Math.min(
                1.0F,
                MAX_IMAGE_DIMENSION / (float) Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, Math.round(source.getWidth() * initialScale));
        int height = Math.max(1, Math.round(source.getHeight() * initialScale));
        float[] qualities = {0.90F, 0.76F, 0.62F, 0.48F, 0.34F, 0.22F};

        for (int resizeAttempt = 0; resizeAttempt < 8; resizeAttempt++) {
            BufferedImage jpegImage = renderRgb(source, width, height);
            for (float quality : qualities) {
                byte[] encoded = encodeJpeg(jpegImage, quality);
                if (encoded.length <= MAX_IMAGE_BYTES) {
                    return encoded;
                }
            }
            width = Math.max(64, Math.round(width * 0.78F));
            height = Math.max(64, Math.round(height * 0.78F));
        }
        throw new IOException("Painting image remains larger than MineAstr's 768 KiB limit after compression");
    }

    private static BufferedImage renderRgb(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return target;
    }

    private static byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer is available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam parameters = writer.getDefaultWriteParam();
        parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parameters.setCompressionQuality(quality);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(image, null, null), parameters);
            imageOutput.flush();
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private static String normalizeLanguage(String language) {
        return language == null
                ? ""
                : language.strip().replace('-', '_').toLowerCase(Locale.ROOT);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static final class TranslationCache extends Cache<TranslationKey, Translation> {
        private static final int FORMAT_VERSION = 1;

        private TranslationCache() {
            super(512);
        }

        @Override
        public String getCachePath(TranslationKey key) {
            return "translations-v1/" + digest(key.imageKey() + "\u0000" + key.language()) + ".bin";
        }

        @Override
        public Translation decode(byte[] bytes) throws IOException {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != FORMAT_VERSION) {
                    throw new IOException("Unsupported MineAstr translation cache version");
                }
                return new Translation(input.readUTF(), input.readUTF());
            }
        }

        @Override
        public byte[] encode(Translation translation) {
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                 DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(FORMAT_VERSION);
                output.writeUTF(translation.translated());
                output.writeUTF(translation.original());
                output.flush();
                return bytes.toByteArray();
            } catch (IOException error) {
                Main.LOGGER.warn("Failed to encode MineAstr translation cache entry", error);
                return null;
            }
        }
    }

    private static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private record TranslationKey(String imageKey, String language) {
    }

    private record Translation(String translated, String original) {
    }
}
