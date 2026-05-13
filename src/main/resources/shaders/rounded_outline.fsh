#version 110

uniform float u_Radius;
uniform float u_Thickness;
uniform vec4 u_InnerRect;

varying vec2 f_Position;

//out vec4 fragColor;

void main() {
    // https://thebookofshaders.com/edit.php
//    vec2 f_Position = gl_FragCoord.xy;
//    vec4 Color = vec4(1.);
//    float u_Radius = 16.;
//    float u_Thickness = 1.;
//    vec4 u_InnerRect = vec4(25.0, 25.0, 475.0, 475.0);

    vec2 tl = u_InnerRect.xy - f_Position;
    vec2 br = f_Position - u_InnerRect.zw;
    vec2 dis = max(br, tl);

    vec2 tl_inner = tl + u_Thickness;
    vec2 br_inner = br + u_Thickness;
    vec2 dis_inner = max(br_inner, tl_inner);

    float v = length(max(vec2(0.), dis)) - u_Radius;

    float radiusAdjust = u_Radius - u_Thickness;
    if (u_Radius < u_Thickness)
    radiusAdjust = u_Radius;

    float v_inner = length(max(vec2(0.), dis_inner)) - radiusAdjust;

    float a = 1.0 - smoothstep(0.0, 1.0, v);
    float a_inner = smoothstep(0.0, 1.0, v_inner);

    gl_FragColor = Color * vec4(1., 1., 1., a * a_inner);
}
