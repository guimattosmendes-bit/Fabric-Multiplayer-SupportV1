package dev.framedfabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;

import dev.framedfabric.FramedFabric;

public class FramedFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Usado nos blockstates: "fabric:type": "framedfabric:framed"
		CustomUnbakedBlockStateModel.register(FramedFabric.id("framed"), FramedBlockStateModel.Unbaked.CODEC);
	}
}
