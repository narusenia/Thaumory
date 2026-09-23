package one.nxeu.thaumory.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import one.nxeu.thaumory.client.ThaumoryClient;

public final class ThaumoryFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ThaumoryClient.init();
    }
}
