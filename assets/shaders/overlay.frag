#version 100

#ifdef GL_ES
    #define LOWP lowp
    precision mediump float;
#else
    #define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_worldPosition;

uniform sampler2D u_texture;
uniform sampler2D u_overlay;
uniform float u_scale;

void main() {
    vec4 baseColor = texture2D(u_texture, v_texCoords);
    vec2 overlayCoords = v_worldPosition * u_scale;
    vec4 overlayColor = texture2D(u_overlay, vec2(overlayCoords.x, 1.0 - overlayCoords.y));
    vec4 finalColor = mix(baseColor, overlayColor, floor(baseColor.r));
    gl_FragColor = v_color * finalColor;
}
