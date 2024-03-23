package io.github.crmodders.flux.api.generators.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import finalforeach.cosmicreach.GameAssetLoader;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.world.BlockPosition;
import finalforeach.cosmicreach.world.World;
import finalforeach.cosmicreach.world.blockevents.BlockEventTrigger;
import finalforeach.cosmicreach.world.blockevents.BlockEvents;
import finalforeach.cosmicreach.world.blockevents.IBlockEventAction;
import finalforeach.cosmicreach.world.blocks.Block;
import finalforeach.cosmicreach.world.blocks.BlockState;
import finalforeach.cosmicreach.world.entities.Player;
import io.github.crmodders.flux.FluxAPI;
import io.github.crmodders.flux.api.blocks.ModBlock;
import io.github.crmodders.flux.api.blocks.WorkingBlock;
import io.github.crmodders.flux.api.registries.BlockReg;
import io.github.crmodders.flux.api.registries.Identifier;
import io.github.crmodders.flux.util.PrivUtils;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.Stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.FileHandler;

public class BlockDataGen {

    public static BlockDataGen createGenerator(Identifier id) {
        return new BlockDataGen(id);
    }

    Identifier id;
    JsonObject Data;

    protected BlockDataGen(Identifier id) {
        this.id = id;
        Data = new JsonObject();
        setID(id.toString())
                .setBlockstate("default", BlockStateDataGen.createGenerator());
    }

    public BlockDataGen setBlockstate(String type, BlockStateDataGen gen) {
        Data.set("blockStates", new JsonObject().set(type, gen.Generate()));
        return this;
    }

    public BlockDataGen setID(String id) {
        Data.set("stringId", id);
        this.id = Identifier.fromString(id);
        return this;
    }

    private void IntegrateToExistingEvents(String NewID, String OldID, WorkingBlock block) {
        FileHandle f = GameAssetLoader.loadAsset("block_events/" + OldID.split(":")[1] + ".json");
        JsonObject jsonObject = JsonObject.readJSON(f.readString()).asObject();
        boolean L_INJECTED_INTERACT = false;
        boolean L_INJECTED_PLACE = false;
        boolean L_INJECTED_BREAK = false;
        for (String s : jsonObject.get("triggers").asObject().names()) {
            if (Objects.equals(s, "onInteract")) {
                L_INJECTED_INTERACT = true;
                jsonObject.set("triggers", jsonObject.get("triggers").asObject()
                        .set(s, jsonObject.get("triggers").asObject().get(s).asArray().add(
                        new JsonObject().set("actionId", NewID+"_INTERACT")
                )));
            }
            if (Objects.equals(s, "onPlace")) {
                L_INJECTED_PLACE = true;
                jsonObject.set("triggers", jsonObject.get("triggers").asObject()
                        .set(s, jsonObject.get("triggers").asObject().get(s).asArray().add(
                                new JsonObject().set("actionId", NewID+"_PLACE")
                        )));
            }
            if (Objects.equals(s, "onBreak")) {
                L_INJECTED_BREAK = true;
                jsonObject.set("triggers", jsonObject.get("triggers").asObject()
                        .set(s, jsonObject.get("triggers").asObject().get(s).asArray().add(
                                new JsonObject().set("actionId", NewID+"_BREAK")
                        )));
            }
        }

        if (!L_INJECTED_INTERACT) jsonObject.set("triggers", jsonObject.get("triggers").asObject()
                    .set("onInteract", jsonObject.get("triggers").asObject().get("onInteract").asArray().add(
                            new JsonObject().set("actionId", NewID+"_INTERACT")
                    )));

        if (!L_INJECTED_PLACE) jsonObject.set("triggers", jsonObject.get("triggers").asObject()
                .set(
                        "onPlace",
                        new JsonArray().add(
                                new JsonObject().set(
                                        "actionId", NewID+"_PLACE"
                                )
                        ).add(
                                new JsonObject().set(
                                        "actionId", "base:replace_block_state"
                                ).set("parameters", new JsonObject()
                                        .set("xOff", 0)
                                        .set("yOff", 0)
                                        .set("zOff", 0)
                                        .set("blockStateId", "self")
                                )
                        ).add(
                                new JsonObject().set(
                                        "actionId", "base:play_sound_2d"
                                ).set("parameters", new JsonObject()
                                        .set("sound", "block-place.ogg")
                                        .set("volume", 1)
                                        .set("pitch", 1)
                                        .set("pan", 0)
                                )
                        )
                ));

        if (!L_INJECTED_BREAK) jsonObject.set("triggers", jsonObject.get("triggers").asObject()
                .set(
                        "onBreak",
                        new JsonArray().add(
                                new JsonObject().set(
                                        "actionId", NewID+"_BREAK"
                                )
                        ).add(
                                new JsonObject().set(
                                        "actionId", "base:replace_block_state"
                                ).set("parameters", new JsonObject()
                                        .set("xOff", 0)
                                        .set("yOff", 0)
                                        .set("zOff", 0)
                                        .set("blockStateId", "base:air[default]")
                                )
                        ).add(
                                new JsonObject().set(
                                        "actionId", "base:play_sound_2d"
                                ).set("parameters", new JsonObject()
                                        .set("sound", "block-break.ogg")
                                        .set("volume", 1)
                                        .set("pitch", 1)
                                        .set("pan", 0)
                                )
                        )
                ));

        BlockEvents.registerBlockEventAction(new IBlockEventAction() {
            @Override
            public String getActionId() {
                return NewID+"_INTERACT";
            }

            @Override
            public void act(BlockState blockState, BlockEventTrigger blockEventTrigger, World world, Map<String, Object> map) {
                try {
                    block.onInteract(
                            world,
                            (Player) PrivUtils.getPrivField(InGame.class, "player"),
                            blockState,
                            (BlockPosition) map.get("blockPos")
                    );
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        BlockEvents.registerBlockEventAction(new IBlockEventAction() {
            @Override
            public String getActionId() {
                return NewID+"_PLACE";
            }

            @Override
            public void act(BlockState blockState, BlockEventTrigger blockEventTrigger, World world, Map<String, Object> map) {
                try {
                    block.onPlace(
                            world,
                            (Player) PrivUtils.getPrivField(InGame.class, "player"),
                            blockState,
                            (BlockPosition) map.get("blockPos")
                    );
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        BlockEvents.registerBlockEventAction(new IBlockEventAction() {
            @Override
            public String getActionId() {
                return NewID+"_BREAK";
            }

            @Override
            public void act(BlockState blockState, BlockEventTrigger blockEventTrigger, World world, Map<String, Object> map) {
                try {
                    block.onBreak(
                            world,
                            (Player) PrivUtils.getPrivField(InGame.class, "player"),
                            blockState,
                            (BlockPosition) map.get("blockPos")
                    );
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        BlockEvents.INSTANCES.put(NewID, new Json().fromJson(BlockEvents.class, jsonObject.toString()));

    }

    private void CreateCustomInteractionEvent(String ID, WorkingBlock block) {
        JsonObject obj = new JsonObject();
        obj.set("parent", "base:block_events_default");
        obj.set("stringId", ID);
        obj.set("triggers", new JsonObject().set(
                "onInteract",
                    new JsonArray().add(
                            new JsonObject().set(
                                    "actionId", ID+"_INTERACT"
                            )
                    )
                ).set(
                "onPlace",
                new JsonArray().add(
                        new JsonObject().set(
                                "actionId", ID+"_PLACE"
                        )
                ).add(
                        new JsonObject().set(
                                "actionId", "base:replace_block_state"
                        ).set("parameters", new JsonObject()
                                .set("xOff", 0)
                                .set("yOff", 0)
                                .set("zOff", 0)
                                .set("blockStateId", "self")
                        )
                ).add(
                        new JsonObject().set(
                                "actionId", "base:play_sound_2d"
                        ).set("parameters", new JsonObject()
                                .set("sound", "block-place.ogg")
                                .set("volume", 1)
                                .set("pitch", 1)
                                .set("pan", 0)
                        )
                )
        ).set(
                "onBreak",
                new JsonArray().add(
                        new JsonObject().set(
                                "actionId", ID+"_BREAK"
                        )
                ).add(
                        new JsonObject().set(
                                "actionId", "base:replace_block_state"
                        ).set("parameters", new JsonObject()
                                .set("xOff", 0)
                                .set("yOff", 0)
                                .set("zOff", 0)
                                .set("blockStateId", "base:air[default]")
                        )
                ).add(
                        new JsonObject().set(
                                "actionId", "base:play_sound_2d"
                        ).set("parameters", new JsonObject()
                                .set("sound", "block-break.ogg")
                                .set("volume", 1)
                                .set("pitch", 1)
                                .set("pan", 0)
                        )
                )
        ));

        BlockEvents.registerBlockEventAction(new IBlockEventAction() {
            @Override
            public String getActionId() {
                return ID+"_INTERACT";
            }

            @Override
            public void act(BlockState blockState, BlockEventTrigger blockEventTrigger, World world, Map<String, Object> map) {
                try {
                    block.onInteract(
                            world,
                            (Player) PrivUtils.getPrivField(InGame.class, "player"),
                            blockState,
                            (BlockPosition) map.get("blockPos")
                    );
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        BlockEvents.registerBlockEventAction(new IBlockEventAction() {
            @Override
            public String getActionId() {
                return ID+"_PLACE";
            }

            @Override
            public void act(BlockState blockState, BlockEventTrigger blockEventTrigger, World world, Map<String, Object> map) {
                try {
                    block.onPlace(
                            world,
                            (Player) PrivUtils.getPrivField(InGame.class, "player"),
                            blockState,
                            (BlockPosition) map.get("blockPos")
                    );
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        BlockEvents.registerBlockEventAction(new IBlockEventAction() {
            @Override
            public String getActionId() {
                return ID+"_BREAK";
            }

            @Override
            public void act(BlockState blockState, BlockEventTrigger blockEventTrigger, World world, Map<String, Object> map) {
                try {
                    block.onBreak(
                            world,
                            (Player) PrivUtils.getPrivField(InGame.class, "player"),
                            blockState,
                            (BlockPosition) map.get("blockPos")
                    );
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        FluxAPI.LOGGER.info("Custom Block Event \""+ID+"\" Created" + Identifier.fromString(ID));
        BlockEvents.INSTANCES.put(ID, new Json().fromJson(BlockEvents.class, obj.toString()));
        FluxAPI.LOGGER.info("Custom Block Event \""+ID+"\" Created" + Identifier.fromString(ID));
    }

    public Block Generate(ModBlock block) {
        if (block instanceof WorkingBlock) {
            JsonObject blockStates = Data.get("blockStates").asObject();
            blockStates.forEach(member -> {
                JsonObject state = blockStates.get(member.getName()).asObject();
                if (state.names().contains("blockEventsId")) {
                    FluxAPI.LOGGER.info(state.get("blockEventsId").toString().replaceAll("\"", ""));
                    IntegrateToExistingEvents(
                            Data.get("stringId").asString().replaceAll("\"", "")
                                    .replaceAll("base:", "test:")
                                    + "_SPECIAL_MODDED_AND_INJECTED_EVENT",
                            state.get("blockEventsId").toString().replaceAll("\"", ""),
                            (WorkingBlock) block
                    );
                    state.set("blockEventsId",
                            Data.get("stringId").asString().replaceAll("\"", "")
                                    .replaceAll("base:", "test:")
                                    + "_SPECIAL_MODDED_AND_INJECTED_EVENT"
                    );
                } else {
                    String CustomBlockEvent = Data.get("stringId").asString().replaceAll("\"", "")+"_INJECTED_EVENT";
                    state.set("blockEventsId", CustomBlockEvent);
                    CreateCustomInteractionEvent(CustomBlockEvent, (WorkingBlock) block);
                }

            });
        }
        FluxAPI.LOGGER.info(Data.toString(Stringify.FORMATTED));
        return BlockReg.getBlockFromJson(
                id,
                Data.toString()
        );
    }

    public static class BlockStateDataGen {
        public static BlockStateDataGen createGenerator() {
            return new BlockStateDataGen();
        }

        JsonObject Data;

        protected BlockStateDataGen() {
            Data = new JsonObject();
            setData( "modelName", JsonValue.valueOf("model_c4"));
        }

        public BlockStateDataGen setData(String key, JsonValue data) {
            Data.set(key, data);
            return this;
        }

        public JsonObject Generate() {
            FluxAPI.LOGGER.info(Data.toString(Stringify.FORMATTED));
            return Data;
        }
    }
}