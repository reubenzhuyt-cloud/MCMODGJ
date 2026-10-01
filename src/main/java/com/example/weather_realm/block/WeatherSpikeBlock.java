package com.example.weather_realm.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 天象尖锥 / Weather Spike - a static, tapering decoration (design §6.4).
 *
 * <p>Carries {@code thickness} (tip/frustum/middle/base), {@code vertical_direction} (up/down)
 * and {@code waterlogged}. Unlike vanilla {@code PointedDripstoneBlock} it has no growth,
 * drip or falling-merge behaviour - it exists purely as a decorative taper and only falls
 * back to air when its support disappears.</p>
 */
public class WeatherSpikeBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<WeatherSpikeBlock> CODEC = simpleCodec(WeatherSpikeBlock::new);

    public static final EnumProperty<SpikeThickness> THICKNESS =
            EnumProperty.create("thickness", SpikeThickness.class);
    public static final DirectionProperty VERTICAL_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * 逐段收窄的碰撞箱，索引与 {@link SpikeThickness} 的 ordinal 一一对应。
     *
     * <p>朝上（{@link Direction#UP}）时 {@code tip} 只占据方块底部（y 0..11），把顶部的
     * 尖端留空；朝下（{@link Direction#DOWN}）时整体沿 Y 轴镜像，{@code tip} 改占顶部
     * （y 5..16），把底部的尖端留空。{@code frustum}/{@code middle}/{@code base} 三段
     * 是贯穿整格（y 0..16）的立柱，关于 y=8 对称，因此其 Y 轴镜像即自身——这与原版
     * {@code PointedDripstoneBlock} 只区分 {@code TIP_SHAPE_UP}/{@code TIP_SHAPE_DOWN}
     * 的做法一致，也是「逐段收窄、无空隙」所要求的（任一段若只占部分高度，堆叠时就会
     * 在相邻两格之间产生空隙）。</p>
     */
    private static final VoxelShape[] SHAPES_UP = new VoxelShape[]{
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 11.0D, 10.0D),   // TIP
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D),   // FRUSTUM
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D),   // MIDDLE
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D),   // BASE
    };

    /** {@link #SHAPES_UP} 沿 Y 轴镜像（y -> 16 - y）后的结果，供 {@link Direction#DOWN} 使用。 */
    private static final VoxelShape[] SHAPES_DOWN = new VoxelShape[]{
            Block.box(6.0D, 5.0D, 6.0D, 10.0D, 16.0D, 10.0D),   // TIP
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D),   // FRUSTUM
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D),   // MIDDLE
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D),   // BASE
    };

    /**
     * 尖锥段层 / Spike thickness: from the narrow {@link #TIP} to the wide {@link #BASE}.
     */
    public enum SpikeThickness implements StringRepresentable {
        TIP("tip"), FRUSTUM("frustum"), MIDDLE("middle"), BASE("base");

        private final String name;

        SpikeThickness(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public WeatherSpikeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(THICKNESS, SpikeThickness.BASE)
                .setValue(VERTICAL_DIRECTION, Direction.UP)
                .setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, VERTICAL_DIRECTION, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape[] shapes = state.getValue(VERTICAL_DIRECTION) == Direction.DOWN
                ? SHAPES_DOWN : SHAPES_UP;
        return shapes[state.getValue(THICKNESS).ordinal()];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Direction vertical = clicked.getAxis() == Direction.Axis.Y ? clicked : Direction.UP;
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(VERTICAL_DIRECTION, vertical)
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(VERTICAL_DIRECTION);
        BlockPos supportPos = pos.relative(direction.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, direction);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == state.getValue(VERTICAL_DIRECTION).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }
}
