package com.lothrazar.library.render.type;

import com.mojang.blaze3d.pipeline.BindGroupLayout;

public enum Samplers {
    SAMPLER_0(BindGroupLayout.builder().withSampler("Sampler0").build()),
    SAMPLER_2(BindGroupLayout.builder().withSampler("Sampler2").build()),;

    private final BindGroupLayout sampler;

    Samplers(BindGroupLayout sampler) {
        this.sampler = sampler;
    }

    public BindGroupLayout sampler() {
        return sampler;
    }
}
