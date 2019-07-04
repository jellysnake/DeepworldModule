/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.deepworld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.naming.Name;
import org.terasology.utilities.Assets;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockBuilderHelper;
import org.terasology.world.block.BlockPart;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.AbstractBlockFamily;
import org.terasology.world.block.family.RegisterBlockFamily;
import org.terasology.world.block.loader.BlockFamilyDefinition;
import org.terasology.world.block.loader.SectionDefinitionData;
import org.terasology.world.block.shapes.BlockShape;
import org.terasology.world.block.tiles.BlockTile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RegisterBlockFamily("tiling")
public class TilingBlockFamily extends AbstractBlockFamily {
    private static final Logger logger = LoggerFactory.getLogger(TilingBlockFamily.class);

    private int size;

    private Block[] blocks;


    public TilingBlockFamily(BlockFamilyDefinition definition, BlockShape shape, BlockBuilderHelper blockBuilder) {
        this(definition, blockBuilder);
    }

    public TilingBlockFamily(BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder) {
        super(definition, blockBuilder);

        SectionDefinitionData data = definition.getData().getBaseSection();
        TiledBlockComponent component = data.getEntity().getPrefab().getComponent(TiledBlockComponent.class);
        size = component.size;

        /* Get the tiles from the component */
        List<BlockTile> frontImages = component.frontTiles.stream()
                .map(urn -> Assets.get(urn, BlockTile.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        List<BlockTile> backImages = component.backTiles.stream()
                .map(urn -> Assets.get(urn, BlockTile.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        /* Make new blocks for each */
        BlockShape cubeShape = Assets.get(CUBE_SHAPE_URN, BlockShape.class).get();
        blocks = new Block[frontImages.size()];
        for (int i = 0; i < frontImages.size(); i++) {
            data.getBlockTiles().put(BlockPart.FRONT, frontImages.get(i));
            data.getBlockTiles().put(BlockPart.BACK, backImages.get(i));
            data.getBlockTiles().put(BlockPart.RIGHT, frontImages.get(i));
            data.getBlockTiles().put(BlockPart.LEFT, backImages.get(i));
            data.getBlockTiles().put(BlockPart.TOP, frontImages.get(i));
            data.getBlockTiles().put(BlockPart.BOTTOM, backImages.get(i));
            blocks[i] = blockBuilder.constructCustomBlock(
                    definition.getUrn().getResourceName().toString(),
                    cubeShape,
                    Rotation.none(),
                    data,
                    new BlockUri(definition.getUrn(), new Name(String.valueOf(i))),
                    this);
        }

    }

    @Override
    public Block getBlockForPlacement(Vector3i location, Side attachmentSide, Side direction) {
        int offsetZ = location.z % size;
        if (offsetZ < 0) {
            offsetZ += size;
        }
        int tileXPos = (location.x + offsetZ) % size;
        if (tileXPos < 0) {
            tileXPos += size;
        }
        int tileYPos = location.y % size;
        if (tileYPos < 0) {
            tileYPos += size;
        }
        int arrayPos = tileYPos * size + tileXPos;
        logger.info("ArrayPos → " + blocks[arrayPos].getURI());
        return blocks[arrayPos];
    }

    @Override
    public Block getArchetypeBlock() {
        return blocks[0];
    }

    @Override
    public Block getBlockFor(BlockUri blockUri) {
        return Arrays.stream(blocks)
                .filter(block -> block.getURI().equals(blockUri))
                .findAny()
                .orElseGet(this::getArchetypeBlock);
    }

    @Override
    public Iterable<Block> getBlocks() {
        return Arrays.asList(blocks);
    }
}
