#version 300 es
precision mediump float;

in vec3 v_Normal;
in vec2 v_TexCoord;

uniform sampler2D u_Texture;
uniform vec3 u_LightPos;       // Position of the light source
uniform vec3 u_CameraPos;      // Position of the camera

layout(location = 0) out vec4 o_FragColor;

void main() {
  vec3 normal = normalize(v_Normal);
  vec3 lightDir = normalize(u_LightPos - vec3(gl_FragCoord.xyz));
  vec3 viewDir = normalize(u_CameraPos - vec3(gl_FragCoord.xyz));

  // Adjust these strength values to increase brightness
  float ambientStrength = 0.7;         // Ambient light
  float diffuseStrength = 10.5;         // Diffuse light multiplier
  float specularStrength = 10.0;        // Specular light multiplier

  // Ambient lighting
  vec3 ambient = ambientStrength * texture(u_Texture, v_TexCoord).rgb;

  // Diffuse lighting
  float diff = max(dot(normal, lightDir), 0.0);
  vec3 diffuse = diff * diffuseStrength * texture(u_Texture, v_TexCoord).rgb;

  // Specular lighting
  vec3 reflectDir = reflect(-lightDir, normal);
  float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
  vec3 specular = specularStrength * spec * vec3(1.0);

  // Combine results
  vec3 result = ambient + diffuse + specular;
  o_FragColor = vec4(result, 1.0);
}