#version 300 es

layout(location = 0) in vec4 a_Position;
layout(location = 1) in vec2 a_TexCoord;
layout(location = 2) in vec3 a_Normal;

uniform mat4 u_ModelViewProjection;
uniform mat4 u_ModelView;
uniform mat4 u_NormalMatrix;

out vec2 v_TexCoord;
out vec3 v_Normal;

void main() {
  v_TexCoord = a_TexCoord;
  v_Normal = mat3(u_NormalMatrix) * a_Normal; // Transform normal to view space
  gl_Position = u_ModelViewProjection * a_Position;
}