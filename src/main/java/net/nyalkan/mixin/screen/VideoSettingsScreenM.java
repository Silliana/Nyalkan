package net.nyalkan.mixin.screen;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.nyalkan.config.gui.NyalkanOptionsScreen;
import net.nyalkan.config.gui.SearchableOptionsList;
import net.nyalkan.config.option.VanillaOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenM extends OptionsSubScreen {
    @Unique private static final int HEADER_HEIGHT = 52;
    @Unique private String nyalkan$query = "";

    protected VideoSettingsScreenM(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    @Override
    protected void addTitle() {
        LinearLayout header = LinearLayout.vertical().spacing(6);
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));

        EditBox search = header.addChild(new EditBox(this.font, 200, 16, Component.translatable("nyalkan.options.search")));
        search.setHint(Component.translatable("nyalkan.options.search"));
        search.setValue(this.nyalkan$query);
        search.setResponder(text -> {
            this.nyalkan$query = text;
            ((SearchableOptionsList) this.list).filter(text);
        });

        this.layout.addToHeader(header);
        this.layout.setHeaderHeight(HEADER_HEIGHT);
    }

    @Override
    protected void addContents() {
        SearchableOptionsList list = this.layout.addToContents(new SearchableOptionsList(this.minecraft, this.width, this));
        this.list = list;
        this.addOptions();
        for (OptionInstance<?> option : VanillaOptions.all())
            list.addSearchOnly(option.createButton(this.options));
        list.filter(this.nyalkan$query);
    }

    @Redirect(method = "addOptions", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/components/OptionsList;addBig(Lnet/minecraft/client/OptionInstance;)V"))
    private void nyalkan$windowMode(OptionsList list, OptionInstance<?> resolution) {
        Button nyalkan = Button.builder(Component.translatable("nyalkan.options.button"),
                b -> this.minecraft.setScreen(new NyalkanOptionsScreen(this, this.options))).build();

        list.addBig(resolution);
        list.addSmall(VanillaOptions.windowMode().createButton(this.options), nyalkan);
    }

    @Redirect(method = "addOptions", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/OptionsList;addSmall([Lnet/minecraft/client/OptionInstance;)V"))
    private void nyalkan$replaceVanilla(OptionsList list, OptionInstance<?>[] options) {
        list.addSmall(VanillaOptions.replaceVanilla(options, this.options));
    }
}
