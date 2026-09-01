package net.nyalkan.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CustomVertexFormat {

    public static final VertexFormatElement ELEMENT_POSITION = new VertexFormatElement(0, 0,VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.POSITION, 4);
    public static final VertexFormatElement ELEMENT_COLOR = new VertexFormatElement(1, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement ELEMENT_UV0 = new VertexFormatElement(2, 0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement ELEMENT_UV2 = new VertexFormatElement(3, 2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);

    public static final VertexFormat COMPRESSED_TERRAIN = VertexFormat.builder()
            .add("Position", ELEMENT_POSITION)
            .add("Color", ELEMENT_COLOR)
            .add("UV0", ELEMENT_UV0)
            .add("UV2", ELEMENT_UV2)
            .build();

    public static final VertexFormat NONE = VertexFormat.builder().build();

    // shared static quad
    public static final VertexFormatElement ELEMENT_PARTICLE_CORNER = new VertexFormatElement(0, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 2);
    public static final VertexFormatElement ELEMENT_PARTICLE_CORNER_UV = new VertexFormatElement(1, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);

    public static final VertexFormat PARTICLE_QUAD = VertexFormat.builder()
            .add("Corner", ELEMENT_PARTICLE_CORNER)
            .add("CornerUV", ELEMENT_PARTICLE_CORNER_UV)
            .build();

    public static final VertexFormatElement ELEMENT_PARTICLE_POS = new VertexFormatElement(2, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);
    public static final VertexFormatElement ELEMENT_PARTICLE_SIZE = new VertexFormatElement(3, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement ELEMENT_PARTICLE_UV_MIN = new VertexFormatElement(4, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement ELEMENT_PARTICLE_UV_MAX = new VertexFormatElement(5, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement ELEMENT_PARTICLE_COLOR = new VertexFormatElement(6, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement ELEMENT_PARTICLE_LIGHT = new VertexFormatElement(7, 0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1);

    public static final VertexFormat PARTICLE_INSTANCE = VertexFormat.builder()
            .add("InstancePos", ELEMENT_PARTICLE_POS)
            .add("InstanceSize", ELEMENT_PARTICLE_SIZE)
            .add("InstanceUVMin", ELEMENT_PARTICLE_UV_MIN)
            .add("InstanceUVMax", ELEMENT_PARTICLE_UV_MAX)
            .add("InstanceColor", ELEMENT_PARTICLE_COLOR)
            .add("InstanceLight", ELEMENT_PARTICLE_LIGHT)
            .build();
}
