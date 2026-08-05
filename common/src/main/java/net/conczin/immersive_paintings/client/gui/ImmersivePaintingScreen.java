package net.conczin.immersive_paintings.client.gui;

import net.conczin.immersive_paintings.ClientPaintingManager;
import net.conczin.immersive_paintings.Main;
import net.conczin.immersive_paintings.Painting;
import net.conczin.immersive_paintings.client.gui.widgets.IntegerSliderWidget;
import net.conczin.immersive_paintings.client.gui.widgets.PaintingWidget;
import net.conczin.immersive_paintings.client.gui.widgets.PercentageSliderWidget;
import net.conczin.immersive_paintings.client.gui.widgets.TexturedButtonWidget;
import net.conczin.immersive_paintings.entity.ImmersivePaintingEntity;
import net.conczin.immersive_paintings.network.LazyNetworkManager;
import net.conczin.immersive_paintings.network.NetworkHandler;
import net.conczin.immersive_paintings.network.payload.c2s.ImageUploadPayload;
import net.conczin.immersive_paintings.network.payload.c2s.PaintingDeletePayload;
import net.conczin.immersive_paintings.network.payload.c2s.PaintingEditPayload;
import net.conczin.immersive_paintings.network.payload.c2s.PaintingRegisterPayload;
import net.conczin.immersive_paintings.registry.Configs;
import net.conczin.immersive_paintings.resources.FrameLoader;
import net.conczin.immersive_paintings.util.ImageManipulations;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.PointerBuffer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImmersivePaintingScreen extends Screen {
    private static final int SCREENSHOTS_PER_PAGE = 5;
    private static final int PAINTINGS_PER_ROW = 8;
    private static final int PAINTING_ROWS = 3;
    private static final int PAINTINGS_PER_PAGE = PAINTINGS_PER_ROW * PAINTING_ROWS;

    private final static ExecutorService service = Executors.newFixedThreadPool(1);

    public final ImmersivePaintingEntity entity;

    private String filteredString = "";
    private int filteredResolution;
    private int filteredWidth;
    private int filteredHeight;
    private final List<Identifier> filteredPaintings = new ArrayList<>();

    private int selectionPage;
    private Page page;

    private Button pageWidget;

    private final Map<Identifier, PaintingWidget> paintingWidgets = new HashMap<>();
    private BufferedImage currentImage;
    private static int currentImagePixelZoomCache = -1;
    private String currentImageName;
    private ImageManipulations.PixelatorSettings settings;
    private BufferedImage pixelatedImage;

    private List<File> screenshots = List.of();
    private int screenshotPage;

    private Identifier deletePainting;
    private int deleteAllConfirmStep;
    private Component error;
    private boolean shouldReProcess;
    private static volatile boolean shouldUpload;


    public ImmersivePaintingScreen(UUID entityId) {
        super(Component.translatable("item.immersive_paintings.painting"));

        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getEntity(entityId) instanceof ImmersivePaintingEntity painting) {
            entity = painting;
        } else {
            entity = null;
        }

        if (entity == null) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        reloadScreenshots();

        if (page == null) {
            // Open directly on the new-painting flow when upload is allowed.
            setPage(canUploadPainting() ? Page.NEW : Page.YOURS);
        } else {
            refreshPage();
        }
    }

    private void reloadScreenshots() {
        File file = new File(Minecraft.getInstance().gameDirectory, "screenshots");
        File[] files = file.listFiles(v -> v.isFile() && v.getName().toLowerCase(Locale.ROOT).endsWith(".png"));
        if (files != null) {
            screenshots = Arrays.stream(files)
                    .sorted(Comparator.comparingLong(File::lastModified).reversed().thenComparing(File::getName))
                    .toList();
        } else {
            screenshots = List.of();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        switch (page) {
            case NEW -> {
                graphics.fill(width / 2 - 115, height / 2 - 68, width / 2 + 115, height / 2 - 41, 0x50000000);
                List<FormattedCharSequence> splits = font.split(Component.translatable("immersive_paintings.gui.drop"), 220);
                int y = height / 2 - 40 - splits.size() * 12;
                for (FormattedCharSequence t : splits) {
                    graphics.drawCenteredString(font, t, width / 2, y, 0xFFFFFFFF);
                    y += 12;
                }
                if (error != null) {
                    graphics.drawCenteredString(font, error, width / 2, height / 2 + 5, 0xFFFF5555);
                }
            }
            case CREATE -> {
                if (shouldReProcess && currentImage != null) {
                    service.submit(this::pixelateImage);
                    shouldReProcess = false;
                }

                if (shouldUpload && pixelatedImage != null) {
                    ClientPaintingManager.newTexture(Main.locate("temp_pixelated"), pixelatedImage);
                }

                int maxWidth = 190;
                int maxHeight = 135;
                int tw = settings.resolution * settings.width;
                int th = settings.resolution * settings.height;
                float size = Math.min((float) maxWidth / tw, (float) maxHeight / th);
                Matrix3x2fStack matrix = graphics.pose();
                matrix.pushMatrix();
                matrix.translate(width / 2.0f - tw * size / 2.0f, height / 2.0f - th * size / 2.0f);
                matrix.scale(size, size);
                graphics.blit(RenderPipelines.GUI_TEXTURED, Main.locate("temp_pixelated"), 0, 0, 0, 0, tw, th, tw, th);
                matrix.popMatrix();

                if (error != null) {
                    graphics.drawCenteredString(font, error, width / 2, height / 2, 0xFFFF0000);
                }
            }
            case DELETE, ADMIN_DELETE -> {
                Component component;
                if (page == Page.DELETE) {
                    component = Component.translatable("immersive_paintings.gui.confirm_deletion");
                } else if (deleteAllConfirmStep > 0) {
                    component = Component.translatable("immersive_paintings.gui.confirm_admin_deletion_progress", deleteAllConfirmStep, 3);
                } else {
                    component = Component.translatable("immersive_paintings.gui.confirm_admin_deletion");
                }

                // Copy drawWordWrap method from GuiGraphics but center the resulting string
                graphics.fill(width / 2 - 160, height / 2 - 50, width / 2 + 160, height / 2 + 50, 0x88000000);
                //graphics.blit(RenderPipelines.GUI_TEXTURED, background, width / 2 - 160, height / 2 - 50, 0, 0, 320, 100, 32, 32);
                int y = height / 2 - 35;
                for (FormattedCharSequence t : font.split(component, 300)) {
                    graphics.drawCenteredString(font, t, width / 2, y, 0xFFFFFFFF);
                    y += 9;
                }


            }
            case LOADING -> {
                Component text = Component.translatable("immersive_paintings.gui.upload", (int) Math.ceil(LazyNetworkManager.getRemainingTime()));
                graphics.drawCenteredString(font, text, width / 2, height / 2, 0xFFFFFFFF);
            }
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private List<Identifier> getMaterialsList(Identifier frame) {
        return FrameLoader.frames.values().stream()
                .filter(v -> v.frame().equals(frame))
                .map(FrameLoader.Frame::material)
                .distinct()
                .sorted(Identifier::compareTo)
                .toList();
    }

    private void rebuild() {
        clearWidgets();

        // filters
        if (page != Page.CREATE) {
            List<Page> b = new LinkedList<>();
            b.add(Page.YOURS);

            if ((Configs.COMMON.showOtherPlayersPaintings && Configs.CLIENT.showOtherPlayersPaintings) || isOp()) {
                b.add(Page.PLAYERS);
            }

            if (canUploadPainting()) {
                b.add(Page.NEW);
            }

            if (!entity.isGraffiti()) {
                b.add(Page.FRAME);
            }

            int x = width / 2 - 200;
            int w = 400 / b.size();
            for (Page page : b) {
                Button btn = addRenderableWidget(
                        Button.builder(
                                Component.translatable("immersive_paintings.gui.page." + page.name().toLowerCase(Locale.ROOT)), sender -> setPage(page))
                        .bounds(x, height / 2 - 90 - 22, w, 20)
                        .build()
                );
                btn.active = page != this.page;
                x += w;
            }
        }

        switch (page) {
            case NEW -> {
                // Native file browser for local images
                addRenderableWidget(Button.builder(
                                Component.translatable("immersive_paintings.gui.browse"), sender -> openFilePicker())
                        .bounds(width / 2 - 100, height / 2 - 38, 200, 20)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.browse.tooltip")))
                        .build()
                );

                // Optional URL input for remote images
                EditBox editBox = addRenderableWidget(new EditBox(font, width / 2 - 100, height / 2 - 12, 145, 16,
                        Component.literal("URL")));
                editBox.setMaxLength(1024);
                editBox.setSuggestion("https://...");

                addRenderableWidget(Button.builder(
                                Component.translatable("immersive_paintings.gui.load"), sender -> {
                                    if (!loadImage(editBox.getValue())) {
                                        setError(Component.translatable("immersive_paintings.error.image_load_failed"));
                                    }
                                })
                        .bounds(width / 2 + 50, height / 2 - 14, 50, 20)
                        .build()
                );

                //screenshots
                rebuildScreenshots();

                //screenshot page
                addRenderableWidget(Button.builder(
                                Component.literal("<<"), sender -> setScreenshotPage(screenshotPage - 1))
                        .bounds(width / 2 - 65, height / 2 + 70, 30, 20)
                        .build()
                );

                pageWidget = addRenderableWidget(Button.builder(
                                Component.literal(""), sender -> {
                                })
                        .bounds(width / 2 - 65 + 30, height / 2 + 70, 70, 20)
                        .build()
                );

                addRenderableWidget(Button.builder(
                                Component.literal(">>"), sender -> setScreenshotPage(screenshotPage + 1))
                        .bounds(width / 2 - 65 + 100, height / 2 + 70, 30, 20)
                        .build()
                );
                setScreenshotPage(screenshotPage);
            }
            case CREATE -> {
                // Name
                EditBox editBox = addRenderableWidget(new EditBox(font, width / 2 - 90, height / 2 - 100, 180, 20,
                        Component.translatable("immersive_paintings.gui.name")));
                editBox.setMaxLength(256);
                editBox.setValue(currentImageName);
                editBox.setResponder(s -> currentImageName = s);

                int y = height / 2 - 60;

                // Width
                addRenderableWidget(new IntegerSliderWidget(width / 2 - 200, y, 100, 20, "immersive_paintings.gui.width", settings.width, 1, 16, v -> {
                    settings.width = v;
                    shouldReProcess = true;
                }));
                y += 22;

                // Height
                addRenderableWidget(new IntegerSliderWidget(width / 2 - 200, y, 100, 20, "immersive_paintings.gui.height", settings.height, 1, 16, v -> {
                    settings.height = v;
                    shouldReProcess = true;
                }));
                y += 22;

                // Resolution
                int x = width / 2 - 200;

                Button resolutionWidget = addRenderableOnly(Button
                        .builder(Component.literal(String.valueOf(settings.resolution)), sender -> {})
                        .pos(x + 25, y)
                        .size(50, 20)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.tooltip.resolution")))
                        .build()
                );

                addRenderableWidget(Button
                        .builder(Component.literal("<"), sender -> {
                            settings.resolution = Math.max(Configs.COMMON.minPaintingResolution, settings.resolution / 2);
                            if (settings.pixelArt) {
                                adaptToPixelArt();
                                refreshPage();
                            }
                            shouldReProcess = true;
                            resolutionWidget.setMessage(Component.literal(String.valueOf(settings.resolution)));
                        })
                        .pos(x, y)
                        .size(25, 20)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.tooltip.resolution")))
                        .build()
                );

                addRenderableWidget(Button
                        .builder(Component.literal(">"), sender -> {
                            settings.resolution = Math.min(Configs.COMMON.maxPaintingResolution, settings.resolution * 2);
                            if (settings.pixelArt) {
                                adaptToPixelArt();
                                refreshPage();
                            }
                            shouldReProcess = true;
                            resolutionWidget.setMessage(Component.literal(String.valueOf(settings.resolution)));
                        })
                        .pos(x + 75, y)
                        .size(25, 20)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.tooltip.resolution")))
                        .build()
                );

                y += 22;
                y += 10;

                // Color reduction
                addRenderableWidget(new IntegerSliderWidget(width / 2 - 200, y, 100, 20, "immersive_paintings.gui.colors", settings.colors, 2, 25, v -> {
                    settings.colors = v;
                    shouldReProcess = true;
                })).active = !settings.pixelArt;
                y += 22;

                // Dither
                addRenderableWidget(new PercentageSliderWidget(width / 2 - 200, y, 100, 20, "immersive_paintings.gui.dither", settings.dither, v -> {
                    settings.dither = v;
                    shouldReProcess = true;
                })).active = !settings.pixelArt;

                // PixelArt
                y = height / 2 - 50;
                addRenderableWidget(Checkbox
                        .builder(Component.translatable("immersive_paintings.gui.pixelart"), font)
                        .pos(width / 2 + 100, y)
                        .selected(settings.pixelArt)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.pixelart.tooltip")))
                        .onValueChange((w, v) -> {
                            settings.pixelArt = v;
                            adaptToPixelArt();
                            refreshPage();
                            shouldReProcess = true;
                        })
                        .build()
                );
                y += 22;

                // Hide
                addRenderableWidget(Checkbox
                        .builder(Component.translatable("immersive_paintings.gui.hide"), font)
                        .pos(width / 2 + 100, y)
                        .selected(settings.hidden)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.tooltip.visibility")))
                        .onValueChange((w, v) -> settings.hidden = !settings.hidden)
                        .build()
                );
                y += 22;

                // NSFW
                addRenderableWidget(Checkbox
                        .builder(Component.translatable("immersive_paintings.gui.nsfw"), font)
                        .pos(width / 2 + 100, y)
                        .selected(settings.nsfw)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.tooltip.nsfw")))
                        .onValueChange((w, v) -> settings.nsfw = !settings.nsfw)
                        .build()
                );
                y += 22;

                // Offset X
                addRenderableWidget(new PercentageSliderWidget(width / 2 + 100, y, 100, 20, "immersive_paintings.gui.x_offset", settings.offsetX, v -> {
                    settings.offsetX = v;
                    shouldReProcess = true;
                }));
                y += 22;

                // Offset Y
                addRenderableWidget(new PercentageSliderWidget(width / 2 + 100, y, 100, 20, "immersive_paintings.gui.y_offset", settings.offsetY, v -> {
                    settings.offsetY = v;
                    shouldReProcess = true;
                }));
                y += 22;

                // Offset
                addRenderableWidget(new PercentageSliderWidget(width / 2 + 100, y, 100, 20, "immersive_paintings.gui.zoom", settings.zoom, entity.isGraffiti() ? 0.5 : 1.0, entity.isGraffiti() ? 1.5 : 3.0, v -> {
                    settings.zoom = v;
                    shouldReProcess = true;
                })).active = !settings.pixelArt;

                // Cancel
                addRenderableWidget(Button.builder(
                                Component.translatable("immersive_paintings.gui.cancel"), v -> setPage(Page.NEW))
                        .bounds(width / 2 - 85, height / 2 + 75, 80, 20)
                        .build()
                );

                // Save
                addRenderableWidget(Button.builder(
                                Component.translatable("immersive_paintings.gui.save"), v -> {
                                    byte[] encoded;

                                    try {
                                        encoded = ImageManipulations.encode(pixelatedImage);
                                    } catch (IOException e) {
                                        Main.LOGGER.error("could not encode temp image", e);
                                        return;
                                    }

                                    ImageManipulations.processByteArrayInChunks(encoded, (ints, split, splits) -> LazyNetworkManager.sendToServer(new ImageUploadPayload(ints, split, splits)));

                                    EnumSet<Painting.Flag> flags = settings.getFlags();
                                    if (entity.isGraffiti())
                                        flags.add(Painting.Flag.GRAFFITI);

                                    // Using LazyNetworkManager here guarantees the register request won't arrive before the image is uploaded
                                    LazyNetworkManager.sendToServer(new PaintingRegisterPayload(
                                            settings.width,
                                            settings.height,
                                            settings.resolution,
                                            currentImageName,
                                            flags
                                    ));

                                    setPage(Page.LOADING);
                                })
                        .bounds(width / 2 + 5, height / 2 + 75, 80, 20)
                        .build()
                );
            }
            case YOURS, PLAYERS -> {
                rebuildPaintings();

                // page
                addRenderableWidget(Button.builder(
                                Component.literal("<<"), sender -> setSelectionPage(selectionPage - 1))
                        .bounds(width / 2 - 35 - 30, height / 2 + 80, 30, 20)
                        .build()
                );

                pageWidget = addRenderableWidget(Button.builder(
                                Component.literal(""), sender -> {
                                })
                        .bounds(width / 2 - 35, height / 2 + 80, 70, 20)
                        .build()
                );

                addRenderableWidget(Button.builder(
                                Component.literal(">>"), sender -> setSelectionPage(selectionPage + 1))
                        .bounds(width / 2 + 35, height / 2 + 80, 30, 20)
                        .build()
                );

                setSelectionPage(selectionPage);

                //search
                EditBox editBox = addRenderableWidget(new EditBox(font, width / 2 - 65, height / 2 - 88, 130, 16, Component.translatable("immersive_paintings.gui.search")));
                editBox.setMaxLength(64);
                editBox.setValue(filteredString);
                editBox.setSuggestion(filteredString.isEmpty() ? "search" : null);
                editBox.setResponder(s -> {
                    filteredString = s;
                    updateSearch();
                    editBox.setSuggestion(null);
                });

                int x = width / 2 - 200 + 12;

                Button widget = addRenderableWidget(Button
                        .builder(Component.literal(String.valueOf(filteredResolution)), sender -> {
                        })
                        .pos(x + 50 + 8, height / 2 - 90)
                        .size(25, 20)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.tooltip.filter_resolution")))
                        .build()
                );
                Button allWidget = addRenderableWidget(Button
                        .builder(Component.translatable("immersive_paintings.gui.filter_all"), sender -> {
                            filteredResolution = 0;
                            updateSearch();
                            widget.setMessage(Component.literal(String.valueOf(filteredResolution)));
                            sender.active = false;
                        })
                        .pos(x, height / 2 - 90)
                        .size(25, 20)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.tooltip.filter_resolution")))
                        .build()
                );
                allWidget.active = filteredResolution != 0;

                addRenderableWidget(Button
                        .builder(Component.literal("<"), sender -> {
                            filteredResolution = filteredResolution == 0 ? 32 : Math.max(Configs.COMMON.minPaintingResolution, filteredResolution / 2);
                            updateSearch();
                            widget.setMessage(Component.literal(String.valueOf(filteredResolution)));
                            allWidget.active = true;
                        })
                        .pos(x + 25 + 8, height / 2 - 90)
                        .size(25, 20)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.tooltip.filter_resolution")))
                        .build()
                );

                addRenderableWidget(Button
                        .builder(Component.literal(">"), sender -> {
                            filteredResolution = filteredResolution == 0 ? 32 : Math.min(Configs.COMMON.maxPaintingResolution, filteredResolution * 2);
                            updateSearch();
                            widget.setMessage(Component.literal(String.valueOf(filteredResolution)));
                            allWidget.active = true;
                        })
                        .pos(x + 75 + 8, height / 2 - 90)
                        .size(25, 20)
                        .tooltip(Tooltip.create(Component.translatable("immersive_paintings.gui.tooltip.filter_resolution")))
                        .build()
                );

                //width
                EditBox widthWidget = addRenderableWidget(new EditBox(font, width / 2 + 80, height / 2 - 88, 40, 16, Component.translatable("immersive_paintings.gui.filter_width")));
                widthWidget.setMaxLength(2);
                widthWidget.setValue(filteredWidth == 0 ? "" : String.valueOf(filteredWidth));
                widthWidget.setSuggestion(filteredWidth == 0 ? "width" : null);
                widthWidget.setResponder(s -> {
                    try {
                        filteredWidth = Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                        filteredWidth = 0;
                    }
                    updateSearch();
                    widthWidget.setSuggestion(null);
                });

                //height
                EditBox heightWidget = addRenderableWidget(new EditBox(font, width / 2 + 80 + 40, height / 2 - 88, 40, 16, Component.translatable("immersive_paintings.gui.filter_height")));
                heightWidget.setMaxLength(2);
                heightWidget.setValue(filteredHeight == 0 ? "" : String.valueOf(filteredHeight));
                heightWidget.setSuggestion(filteredHeight == 0 ? "height" : null);
                heightWidget.setResponder(s -> {
                    try {
                        filteredHeight = Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                        filteredHeight = 0;
                    }
                    updateSearch();
                    heightWidget.setSuggestion(null);
                });
            }
            case FRAME -> {
                //frame
                int y = height / 2 - 80;
                List<Identifier> frames = FrameLoader.frames.values().stream().map(FrameLoader.Frame::frame).distinct().sorted(Identifier::compareTo).toList();
                for (Identifier frame : frames) {
                    Button widget = addRenderableWidget(Button.builder(
                                    Component.translatable("immersive_paintings.frame." + identifierToTranslation(frame)), v -> {
                                        Identifier material = getMaterialsList(frame).getFirst();

                                        // TODO
                                        // This is needed so that when the GUI updates it has the right frame and material set
                                        // I don't like having to set it here, there should be a better way
                                        entity.setFrame(frame);
                                        entity.setMaterial(material);
                                        NetworkHandler.Client.sendToServer(new PaintingEditPayload(entity.getUUID(), Map.of(
                                                PaintingEditPayload.Option.FRAME, frame.toString(),
                                                PaintingEditPayload.Option.MATERIAL, material.toString()
                                        )));
                                        setPage(Page.FRAME);
                                    })
                            .bounds(width / 2 - 200, y, 100, 20)
                            .build()
                    );
                    widget.active = !frame.equals(entity.getFrame());
                    y += 25;
                }

                //material
                int py = 0;
                int px = 0;
                List<Identifier> materials = getMaterialsList(entity.getFrame());
                List<Button> materialList = new LinkedList<>();
                for (Identifier material : materials) {
                    Button widget = addRenderableWidget(new TexturedButtonWidget(
                            width / 2 - 80 + px * 65, height / 2 - 80 + py * 20, 64, 16,
                            Identifier.fromNamespaceAndPath(material.getNamespace(), material.getPath().replace("/block/", "/gui/")),
                            64, 32,
                            Component.literal(""),
                            v -> {
                                entity.setMaterial(material);
                                NetworkHandler.Client.sendToServer(new PaintingEditPayload(entity.getUUID(), Map.of(
                                        PaintingEditPayload.Option.MATERIAL, material.toString()
                                )));
                                materialList.forEach(b -> b.active = true);
                                v.active = false;
                            }
                    ));
                    Tooltip paintingTooltip = Tooltip.create(Component.translatable("immersive_paintings.material." + identifierToTranslation(material)));
                    widget.setTooltip(paintingTooltip);

                    widget.active = !material.equals(entity.getMaterial());
                    materialList.add(widget);

                    px++;
                    if (px > 3) {
                        px = 0;
                        py++;
                    }
                }

                addRenderableWidget(Button.builder(
                                Component.translatable("immersive_paintings.gui.done"), v -> onClose())
                        .bounds(width / 2 - 50, height / 2 + 70, 100, 20)
                        .build()
                );
            }
            case DELETE, ADMIN_DELETE -> {
                int w = page == Page.ADMIN_DELETE ? 90 : 100;
                int h = page == Page.ADMIN_DELETE ? 10 : 20;
                int start = page == Page.ADMIN_DELETE ? -145 : -105;
                Page p = page == Page.ADMIN_DELETE ? Page.PLAYERS : Page.YOURS;

                List<Button.Builder> buttonList = new ArrayList<>();
                buttonList.add(Button.builder(Component.translatable("immersive_paintings.gui.cancel"), v -> {
                    deleteAllConfirmStep = 0;
                    setPage(p);
                }));
                buttonList.add(Button.builder(Component.translatable("immersive_paintings.gui.delete"), v -> {
                    NetworkHandler.Client.sendToServer(new PaintingDeletePayload(deletePainting, false));
                    setPage(p);
                }));

                if (page == Page.ADMIN_DELETE) {
                    int remaining = 3 - deleteAllConfirmStep;
                    Component deleteAllLabel = deleteAllConfirmStep == 0
                            ? Component.translatable("immersive_paintings.gui.delete_all")
                            : Component.translatable("immersive_paintings.gui.delete_all_confirm", remaining);
                    buttonList.add(Button.builder(deleteAllLabel, v -> {
                        deleteAllConfirmStep++;
                        if (deleteAllConfirmStep >= 3) {
                            NetworkHandler.Client.sendToServer(new PaintingDeletePayload(deletePainting, true));
                            deleteAllConfirmStep = 0;
                            setPage(Page.PLAYERS);
                        } else {
                            // Rebuild so the button label updates with remaining confirms
                            rebuild();
                        }
                    }));
                }

                for (int i = 0; i < buttonList.size(); i++) {
                    addRenderableWidget(buttonList.get(i).bounds(width / 2 + start + i * (w + 5), height / 2 + h, w, 20).build());
                }
            }
        }
    }

    public void updateWidget(Identifier identifier) {
        if (paintingWidgets.containsKey(identifier)) {
            ClientPaintingManager.getPainting(identifier).ifPresent(p -> paintingWidgets.get(identifier).update(ClientPaintingManager.getImageIdentifier(identifier, Painting.Size.THUMBNAIL), p.width(), p.height()));
        }
    }

    public static String identifierToTranslation(Identifier location) {
        String s = location.getPath();
        String lastSplit = s.substring(s.lastIndexOf("/") + 1);

        int i = lastSplit.lastIndexOf(".");
        return i < 0 ? lastSplit : lastSplit.substring(0, i);
    }

    private static Component consolidate(List<Component> textList) {
        if (textList == null)
            return null;

        Component base = Component.empty();
        MutableComponent lastTextNode = base.copy();

        if (textList.isEmpty())
            return base;

        for (int i = 0; i < textList.size() - 1; i++) {
            Component text = textList.get(i);
            lastTextNode = lastTextNode.append(text).append("\n");
        }

        Component finalElement = textList.getLast();
        return lastTextNode.append(finalElement);
    }

    private void rebuildPaintings() {
        paintingWidgets.forEach((id, widget) -> removeWidget(widget));
        paintingWidgets.clear();

        // paintings
        for (int y = 0; y < PAINTING_ROWS; y++) {
            for (int x = 0; x < PAINTINGS_PER_ROW; x++) {
                int i = y * PAINTINGS_PER_ROW + x + selectionPage * PAINTINGS_PER_PAGE;
                if (i >= 0 && i < filteredPaintings.size()) {
                    Identifier identifier = filteredPaintings.get(i);

                    //tooltip
                    List<Component> tooltip = new LinkedList<>();

                    ClientPaintingManager.getPainting(identifier).ifPresent(p -> {
                        tooltip.add(Component.literal(p.name()));
                        tooltip.add(Component.translatable("immersive_paintings.gui.by_author", p.author()).withStyle(ChatFormatting.ITALIC));
                        tooltip.add(Component.translatable("immersive_paintings.gui.resolution", p.width(), p.height(), p.resolution()).withStyle(ChatFormatting.ITALIC));

                        if (page == Page.YOURS && p.has(Painting.Flag.HIDDEN)) {
                            tooltip.add(Component.translatable("immersive_paintings.gui.hidden").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
                        }

                        if (page == Page.YOURS && p.has(Painting.Flag.NSFW)) {
                            tooltip.add(Component.translatable("immersive_paintings.gui.nsfw").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
                        }

                        if (page == Page.YOURS || page == Page.PLAYERS && isOp()) {
                            tooltip.add(Component.translatable("immersive_paintings.gui.right_click_to_delete").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
                        }
                    });

                    PaintingWidget paintingWidget = addRenderableWidget(new PaintingWidget(
                            (int) (width / 2.0 + (x - 3.5) * 48) - 24, height / 2 - 66 + y * 48, 46, 46,
                            sender -> {
                                NetworkHandler.Client.sendToServer(new PaintingEditPayload(entity.getUUID(), Map.of(
                                        PaintingEditPayload.Option.MOTIVE, identifier.toString()
                                )));
                                if (entity.isGraffiti()) {
                                    onClose();
                                } else {
                                    setPage(Page.FRAME);
                                }
                            },
                            sender -> {
                                if (page == Page.YOURS) {
                                    deletePainting = identifier;
                                    setPage(Page.DELETE);
                                } else if (page == Page.PLAYERS && isOp()) {
                                    deletePainting = identifier;
                                    deleteAllConfirmStep = 0;
                                    setPage(Page.ADMIN_DELETE);
                                }
                            }
                    ));

                    paintingWidget.setTooltip(Tooltip.create(consolidate(tooltip)));

                    paintingWidgets.put(identifier, paintingWidget);
                    updateWidget(identifier);
                } else {
                    break;
                }
            }
        }
    }

    private void rebuildScreenshots() {
        paintingWidgets.forEach((id, widget) -> removeWidget(widget));
        paintingWidgets.clear();

        // screenshots
        for (int x = 0; x < SCREENSHOTS_PER_PAGE; x++) {
            int i = x + screenshotPage * SCREENSHOTS_PER_PAGE;
            if (i >= 0 && i < screenshots.size()) {
                File file = screenshots.get(i);

                PaintingWidget paintingWidget = addRenderableWidget(new PaintingWidget(
                        (width / 2 + (x - SCREENSHOTS_PER_PAGE / 2) * 68) - 32, height / 2 + 15, 64, 48,
                        b -> {
                            currentImage = ((PaintingWidget) b).getImage();
                            if (currentImage != null) {
                                currentImagePixelZoomCache = -1;
                                currentImageName = file.getName();
                                settings = new ImageManipulations.PixelatorSettings(currentImage);
                                setPage(Page.CREATE);
                                pixelateImage();
                            }
                        },
                        b -> {}
                ));

                paintingWidget.setTooltip(Tooltip.create(Component.literal(file.getName())));

                Identifier identifier = Main.locate("screenshot_" + i);
                paintingWidgets.put(identifier, paintingWidget);

                service.submit(() -> paintingWidget.update(identifier, loadImage(file.getPath(), identifier)));
            } else {
                break;
            }
        }
    }

    public void setPage(Page page) {
        Page previousPage = this.page;
        this.page = page;
        if (page != previousPage && isPaintingSelectionPage(page)) {
            resetFilters();
            selectionPage = 0;
        }

        rebuild();

        if (isPaintingSelectionPage(page)) {
            updateSearch();
        }
    }

    private static boolean isPaintingSelectionPage(Page page) {
        return page == Page.PLAYERS || page == Page.YOURS;
    }

    private void resetFilters() {
        filteredString = "";
        filteredResolution = 0;
        filteredWidth = 0;
        filteredHeight = 0;
    }

    private void updateSearch() {
        filteredPaintings.clear();

        LocalPlayer player = Minecraft.getInstance().player;
        UUID uuid = player == null ? null : player.getUUID();
        filteredPaintings.addAll(ClientPaintingManager.getPaintings().entrySet().stream()
                .filter(e -> {
                    Painting p = e.getValue();
                    return (
                                   (page == Page.YOURS && !p.is(Painting.Type.DATAPACK) && p.authorUUID().equals(uuid)) ||
                                   (page == Page.PLAYERS && !p.is(Painting.Type.DATAPACK) && (!p.has(Painting.Flag.HIDDEN) || isOp()) && (!p.has(Painting.Flag.NSFW) || Configs.CLIENT.showNSFWPaintings || isOp())) ||
                                   (page == Page.DATAPACKS && p.is(Painting.Type.DATAPACK))
                           ) &&
                           p.has(Painting.Flag.GRAFFITI) == entity.isGraffiti() &&
                           e.getKey().toString().contains(filteredString) &&
                           (filteredResolution == 0 || p.resolution() == filteredResolution) &&
                           (filteredWidth == 0 || p.width() == filteredWidth) &&
                           (filteredHeight == 0 || p.height() == filteredHeight);
                })
                .sorted(Comparator.comparing(p -> p.getValue().name()))
                .map(Map.Entry::getKey)
                .toList());

        setSelectionPage(selectionPage);
    }

    private boolean isOp() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        return player.permissions().hasPermission(Permissions.COMMANDS_OWNER);
    }

    private static boolean canUploadPainting() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        PermissionLevel level = PermissionLevel.byId(Configs.COMMON.uploadPermissionLevel);
        return level == PermissionLevel.ALL || player.permissions().hasPermission(new Permission.HasCommandLevel(level));
    }

    private void setSelectionPage(int p) {
        int maxPages = pageCount(filteredPaintings.size(), PAINTINGS_PER_PAGE);
        selectionPage = clampPage(p, maxPages);
        rebuildPaintings();
        pageWidget.setMessage(pageMessage(selectionPage, maxPages));
    }

    private void setScreenshotPage(int p) {
        int maxPages = pageCount(screenshots.size(), SCREENSHOTS_PER_PAGE);
        int oldPage = screenshotPage;
        screenshotPage = clampPage(p, maxPages);
        if (oldPage != screenshotPage) {
            rebuildScreenshots();
        }
        pageWidget.setMessage(pageMessage(screenshotPage, maxPages));
    }

    private static int pageCount(int itemCount, int itemsPerPage) {
        return (itemCount + itemsPerPage - 1) / itemsPerPage;
    }

    private static int clampPage(int requestedPage, int pageCount) {
        return pageCount == 0 ? 0 : Math.clamp(requestedPage, 0, pageCount - 1);
    }

    private static Component pageMessage(int currentPage, int pageCount) {
        return Component.literal(pageCount == 0 ? "0 / 0" : (currentPage + 1) + " / " + pageCount);
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        for (Path path : paths) {
            if (path == null) continue;

            String p = path.toString();
            if (p.isEmpty()) continue;

            if (!Files.exists(path)) continue;

            if (loadImage(p)) {
                return;
            }
        }

        setError(Component.translatable("immersive_paintings.error.image_load_failed"));
    }


    private void openFilePicker() {
        // Immediate UI feedback; native dialogs can take a moment.
        setError(Component.translatable("immersive_paintings.gui.browse.opening"));

        // Release mouse grab first so the OS dialog can sit above GLFW and accept input.
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().mouseHandler != null) {
                Minecraft.getInstance().mouseHandler.releaseMouse();
            }

            // Dedicated thread so the client render thread is not blocked by the modal dialog.
            Thread thread = new Thread(() -> {
                String selected = null;
                Exception failure = null;
                try {
                    // Prefer LWJGL tinyfd: works with Minecraft's GLFW window on Windows.
                    selected = openWithTinyFileDialog();
                } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                    Main.LOGGER.warn("tinyfd unavailable, falling back to AWT file dialog", e);
                    try {
                        selected = openWithAwtFileDialog();
                    } catch (Exception awtFailure) {
                        failure = awtFailure;
                        Main.LOGGER.error("Failed to open AWT file dialog", awtFailure);
                    }
                } catch (Exception e) {
                    failure = e;
                    Main.LOGGER.error("Failed to open native file dialog", e);
                }

                final String path = selected;
                final Exception errorToReport = failure;
                Minecraft.getInstance().execute(() -> {
                    if (path != null && !path.isBlank()) {
                        setError(null);
                        if (!loadImage(path)) {
                            setError(Component.translatable("immersive_paintings.error.image_load_failed"));
                        }
                    } else if (errorToReport != null) {
                        setError(Component.translatable("immersive_paintings.error.file_dialog_failed"));
                    } else {
                        // User cancelled or dialog returned nothing.
                        setError(null);
                    }
                });
            }, "immersive-paintings-file-dialog");
            thread.setDaemon(true);
            thread.start();
        });
    }

    private static String openWithTinyFileDialog() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(6);
            filters.put(stack.UTF8("*.png"));
            filters.put(stack.UTF8("*.jpg"));
            filters.put(stack.UTF8("*.jpeg"));
            filters.put(stack.UTF8("*.gif"));
            filters.put(stack.UTF8("*.bmp"));
            filters.put(stack.UTF8("*.webp"));
            filters.flip();

            String title = Component.translatable("immersive_paintings.gui.browse").getString();
            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    title,
                    "",
                    filters,
                    "Images",
                    false
            );
            if (result == null || result.isBlank()) {
                return null;
            }
            // tinyfd can return multiple paths separated by '|'; we only take the first.
            int sep = result.indexOf('|');
            return sep >= 0 ? result.substring(0, sep) : result;
        }
    }

    private static String openWithAwtFileDialog() {
        try {
            // Only effective before AWT toolkit init; still attempt for non-headless launches.
            System.setProperty("java.awt.headless", "false");

            java.awt.FileDialog dialog = new java.awt.FileDialog((java.awt.Frame) null,
                    Component.translatable("immersive_paintings.gui.browse").getString(),
                    java.awt.FileDialog.LOAD);
            dialog.setFilenameFilter((dir, name) -> {
                String lower = name.toLowerCase(Locale.ROOT);
                return lower.endsWith(".png")
                        || lower.endsWith(".jpg")
                        || lower.endsWith(".jpeg")
                        || lower.endsWith(".gif")
                        || lower.endsWith(".bmp")
                        || lower.endsWith(".webp");
            });
            dialog.setMultipleMode(false);
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);

            String file = dialog.getFile();
            String directory = dialog.getDirectory();
            if (file == null || directory == null) {
                return null;
            }
            return new File(directory, file).getAbsolutePath();
        } catch (Throwable t) {
            Main.LOGGER.error("AWT file dialog failed", t);
            throw new RuntimeException(t);
        }
    }

    private boolean loadImage(String path) {
        currentImage = loadImage(path, Main.locate("temp"));
        currentImagePixelZoomCache = -1;
        if (currentImage != null) {
            currentImageName = toFileName(path);
            settings = new ImageManipulations.PixelatorSettings(currentImage);
            setPage(Page.CREATE);
            pixelateImage();
            return true;
        }
        return false;
    }

    private String toFileName(String path) {
        path = path.replace("\\", "/");
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        if (lastDot < lastSlash) lastDot = path.length(); // no extension
        return path.substring(lastSlash + 1, lastDot);
    }

    private BufferedImage loadImage(String path, Identifier identifier) {
        InputStream stream = null;
        try {
            URLConnection connection = new URL(path).openConnection();
            connection.setRequestProperty("User-Agent", "Main/1.0");
            stream = connection.getInputStream();
        } catch (Exception exception) {
            try {
                stream = new FileInputStream(path);
            } catch (Exception e) {
                Main.LOGGER.error("failed loading image {} from path {}", identifier, path, e);
            }
        }

        if (stream != null) {
            try {
                BufferedImage image = ImageIO.read(stream);
                if (image != null) {
                    // Attempt to preprocess by checking for transparency on non-graffiti paintings
                    setError(null);
                    if (!entity.isGraffiti()) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            for (int y = 0; y < image.getHeight(); y++) {
                                int color = image.getRGB(x, y);
                                if (((color >> 24) & 255) != 255) {
                                    if (error == null)
                                        setError(Component.translatable("immersive_paintings.gui.graffiti_warning"));
                                    image.setRGB(x, y, (255 << 24) | (color & 0x00ffffff));
                                }
                            }
                        }
                    }

                    ClientPaintingManager.newTexture(identifier, image);
                    stream.close();
                    return image;
                }
            } catch (IOException e) {
                Main.LOGGER.error("failed decoding image {} from path {}", identifier, path, e);
            }
        }

        return null;
    }

    private void adaptToPixelArt() {
        double zoom = currentImagePixelZoomCache = ImageManipulations.getCurrentImagePixelZoomCache(currentImage, currentImagePixelZoomCache);
        settings.width = Math.max(1, Math.min(16, (int) (currentImage.getWidth() / zoom / settings.resolution)));
        settings.height = Math.max(1, Math.min(16, (int) (currentImage.getHeight() / zoom / settings.resolution)));
    }

    private void pixelateImage() {
        if (settings.pixelArt)
            currentImagePixelZoomCache = ImageManipulations.getCurrentImagePixelZoomCache(currentImage, currentImagePixelZoomCache);
        pixelatedImage = ImageManipulations.pixelateImage(currentImage, settings, currentImagePixelZoomCache);
        shouldUpload = true;
    }

    public void refreshPage() {
        setPage(page);
    }

    public void setError(Component text) {
        error = text;
    }

    public enum Page {
        YOURS,
        DATAPACKS,
        PLAYERS,
        NEW,
        CREATE,
        FRAME,
        DELETE,
        ADMIN_DELETE,
        LOADING
    }
}
