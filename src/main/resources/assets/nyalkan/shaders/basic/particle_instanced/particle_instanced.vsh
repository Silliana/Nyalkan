#version 450

#include "fog.glsl"

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    vec4 ParticleRight;
    vec4 ParticleUp;
    vec4 ParticleOffset;
};

layout(binding = 3) uniform sampler2D Sampler2;

layout(location = 0) in vec2 Corner;
layout(location = 1) in vec3 PrevPos;
layout(location = 2) in vec3 Pos;
layout(location = 3) in vec4 SizeRoll;
layout(location = 4) in vec4 UVRect;
layout(location = 5) in vec4 Color;
layout(location = 6) in ivec2 Light;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord0;
layout(location = 2) out float vertexDistance;

void main() {
    float partialTick = ParticleOffset.w;
    vec3 center = mix(PrevPos, Pos, partialTick) + ParticleOffset.xyz;
    float size = mix(SizeRoll.x, SizeRoll.y, partialTick);
    float roll = mix(SizeRoll.z, SizeRoll.w, partialTick);

    float c = cos(roll);
    float s = sin(roll);
    vec3 right = c * ParticleRight.xyz + s * ParticleUp.xyz;
    vec3 up = c * ParticleUp.xyz - s * ParticleRight.xyz;

    vec3 position = center + (Corner.x * right + Corner.y * up) * size;
    gl_Position = MVP * vec4(position, 1.0);

    vertexDistance = fog_distance(position, 0);
    texCoord0 = vec2(Corner.x > 0.0 ? UVRect.z : UVRect.x, Corner.y > 0.0 ? UVRect.y : UVRect.w);
    vertexColor = Color * texelFetch(Sampler2, Light / 16, 0);
}
