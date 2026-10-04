package net.nyalkan.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CustomVertexFormat {

    public static final VertexFormatElement ELEMENT_POSITION = new VertexFormatElement(0, 0,VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.POSITION, 4);
    public static final VertexFormatElement ELEMENT_COLOR = new VertexFormatElement(1, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement ELEMENT_UV0 = new VertexFormatElement(2, 0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 2);

    public static final VertexFormat COMPRESSED_TERRAIN = VertexFormat.builder()
            .add("Position", ELEMENT_POSITION)
            .add("Color", ELEMENT_COLOR)
            .add("UV0", ELEMENT_UV0)
            .build();

    public static final VertexFormat NONE = VertexFormat.builder().build();

    public static final VertexFormatElement ELEMENT_PARTICLE_CORNER = new VertexFormatElement(0, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);

    public static final VertexFormat PARTICLE_QUAD = VertexFormat.builder()
            .add("Corner", ELEMENT_PARTICLE_CORNER)
            .build();

    public static final VertexFormatElement ELEMENT_PARTICLE_PREV_POS = new VertexFormatElement(1, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);
    public static final VertexFormatElement ELEMENT_PARTICLE_POS = new VertexFormatElement(2, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);
    public static final VertexFormatElement ELEMENT_PARTICLE_SIZE_ROLL = new VertexFormatElement(3, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
    public static final VertexFormatElement ELEMENT_PARTICLE_UV_RECT = new VertexFormatElement(4, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
    public static final VertexFormatElement ELEMENT_PARTICLE_COLOR = new VertexFormatElement(5, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement ELEMENT_PARTICLE_LIGHT = new VertexFormatElement(6, 0, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);

    public static final VertexFormat PARTICLE_INSTANCE = VertexFormat.builder()
            .add("PrevPos", ELEMENT_PARTICLE_PREV_POS)
            .add("Pos", ELEMENT_PARTICLE_POS)
            .add("SizeRoll", ELEMENT_PARTICLE_SIZE_ROLL)
            .add("UVRect", ELEMENT_PARTICLE_UV_RECT)
            .add("Color", ELEMENT_PARTICLE_COLOR)
            .add("Light", ELEMENT_PARTICLE_LIGHT)
            .build();
}
