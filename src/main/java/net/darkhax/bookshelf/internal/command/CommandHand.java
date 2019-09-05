package net.darkhax.bookshelf.internal.command;

import java.util.function.Function;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.darkhax.bookshelf.Bookshelf;
import net.darkhax.bookshelf.command.ArgumentTypeEnum;
import net.darkhax.bookshelf.internal.network.PacketSetClipboard;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IngredientNBT.Serializer;

public class CommandHand {
    
    public CommandHand(LiteralArgumentBuilder<CommandSource> root) {
        
        root.then(Commands.literal("hand").then(Commands.argument("type", ArgumentTypeEnum.enumArgument(OutputType.class)).then(Commands.argument("clipboard", BoolArgumentType.bool()).executes(this::hand))));
    }
    
    private int hand (CommandContext<CommandSource> context) throws CommandSyntaxException {
         
        OutputType type = context.getArgument("type", OutputType.class);
        boolean useClipboard = BoolArgumentType.getBool(context, "clipboard");
        
        ServerPlayerEntity player = context.getSource().asPlayer();
        String outputText = type.converter.apply(player.getHeldItemMainhand());        
        context.getSource().sendFeedback(new StringTextComponent(outputText), true);
        
        if (useClipboard) {
            
            Bookshelf.NETWORK.sendToPlayer(player, new PacketSetClipboard(outputText));
        }
        
        return 0;
    }
    
    public enum OutputType {
        
        STRING("string", stack -> stack.toString()),
        JSON("json", stack -> {
            
            JsonObject json = new JsonObject();
            json.addProperty("type", CraftingHelper.getID(Serializer.INSTANCE).toString());
            json.addProperty("item", stack.getItem().getRegistryName().toString());
            json.addProperty("count", stack.getCount());
            if (stack.hasTag()) {
                json.addProperty("nbt", stack.getTag().toString());
            }
            return json.toString();
            
        }),
        ID("id", stack -> stack.getItem().getRegistryName().toString());
        
        
        private final String name;
        private final Function<ItemStack, String> converter;
        
        OutputType(String name, Function<ItemStack, String> converter) {
            
            this.name = name;
            this.converter = converter;
        }
    }
}