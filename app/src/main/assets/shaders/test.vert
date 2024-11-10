#version 300 es
layout(location = 0) in vec4 a_Position; // X, Y, Z, W
layout(location = 1) in vec2 a_TexCoord; // U, V

uniform mat4 u_ModelViewProjection;

out vec2 v_TexCoord;

void main() {
  gl_Position = u_ModelViewProjection * a_Position;
  v_TexCoord = a_TexCoord;
}