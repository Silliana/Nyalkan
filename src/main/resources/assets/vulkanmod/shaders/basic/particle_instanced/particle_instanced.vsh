#version 460

layout (binding = 0) uniform UniformBufferObject {
    mat4 MVP;
};

layout (location = 0) in vec2 Corner;
layout (location = 1) in vec2 CornerUV;

layout (location = 2) in vec3 InstancePos;
layout (location = 3) in vec2 InstanceSize;
layout (location = 4) in vec2 InstanceUVMin;
layout (location = 5) in vec2 InstanceUVMax;
layout (location = 6) in vec4 InstanceColor;
layout (location = 7) in int InstanceLight;

layout (location = 0) out vec4 vertexColor;
layout (location = 1) out vec2 texCoord0;

void main() {
    vec4 center = MVP * vec4(InstancePos, 1.0);
    gl_Position = center + vec4(Corner * InstanceSize, 0.0, 0.0) * center.w;

    vertexColor = InstanceColor;
    texCoord0 = mix(InstanceUVMin, InstanceUVMax, CornerUV);
}
