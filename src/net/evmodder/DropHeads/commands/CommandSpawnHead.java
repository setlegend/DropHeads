package net.evmodder.DropHeads.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import net.evmodder.DropHeads.DropHeads;
import net.evmodder.EvLib.EvCommand;
import net.evmodder.EvLib.extras.HeadUtils;
import net.evmodder.EvLib.extras.WebUtils;

public class CommandSpawnHead extends EvCommand{
	final private DropHeads pl;
	final boolean SHOW_GRUMM_IN_TAB_COMPLETE;
	final boolean SHOW_GRUMM_IN_TAB_COMPLETE_FOR_BAR = true;

	public CommandSpawnHead(DropHeads plugin) {
		super(plugin);
		pl = plugin;
		SHOW_GRUMM_IN_TAB_COMPLETE = pl.getConfig().getBoolean("show-grumm-in-tab-complete", false);
	}

	String nameFromType(Material type){
		StringBuilder builder = new StringBuilder("");
		boolean lower = false;
		for(char c : type.toString().toCharArray()){
			if(c == '_'){builder.append(' '); lower = false;}
			else if(lower){builder.append(Character.toLowerCase(c));}
			else{builder.append(c); lower = true;}
		}
		return builder.toString();
	}

	//TODO: move to EntityUtils?
	static EntityType getEntityByName(String name){
		//TODO: improve this function / test for errors
		if(name.toUpperCase().startsWith("MHF_")) name = HeadUtils.normalizedNameFromMHFName(name);
		name = name.toUpperCase().replace(' ', '_');
		String noUnderscoresName = name.replace("_", "");
		if(noUnderscoresName.equals("ZOMBIEPIGMAN")) name = "PIG_ZOMBIE";
		else if(noUnderscoresName.equals("MOOSHROOM")) name = "MUSHROOM_COW";

		try{EntityType type = EntityType.valueOf(name); return type;}
		catch(IllegalArgumentException ex){}
		for(EntityType t : EntityType.values()) if(t.name().replace("_", "").equals(noUnderscoresName)) return t;
		return EntityType.UNKNOWN;
	}

	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length == 1 && sender instanceof Player){
			final List<String> tabCompletes = new ArrayList<String>();
			args[0] = args[0].toUpperCase();
			for(String key : pl.getAPI().getTextures().keySet()){
				if(key.startsWith(args[0]) &&
						(SHOW_GRUMM_IN_TAB_COMPLETE || !key.endsWith("|GRUMM") ||
								(SHOW_GRUMM_IN_TAB_COMPLETE_FOR_BAR && args[0].contains(key.substring(0, key.length()-5))))
				) tabCompletes.add(key);
			}
			return tabCompletes;
		}
		return null;
	}

	@SuppressWarnings("deprecation") @Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players!");
			return true;
		}

		String textureKey = args.length == 0 ? sender.getName() : String.join("_", args).replace(':', '|');
		String target, extraData;
		int i = textureKey.indexOf('|');
		if(i != -1){
			target = textureKey.substring(0, i);
			extraData = textureKey.substring(i+1).toUpperCase();
		}
		else{
			target = textureKey;
			extraData = null;
		}
		ItemStack head = null;

		if(pl.getAPI().textureExists(target.toUpperCase())){
			EntityType eType = getEntityByName(target.toUpperCase());
			if(eType != null && eType != EntityType.UNKNOWN){
				textureKey = eType.name() + (extraData == null ? "" : "|"+extraData);
			}
			if(extraData != null){
				if(pl.getAPI().textureExists(textureKey))
					sender.sendMessage(ChatColor.GRAY+"Getting entity head with data value: "+extraData);
				else
					sender.sendMessage(ChatColor.RED+"Unknown data value for "+eType+": "+ChatColor.YELLOW+extraData);
			}
			head = pl.getAPI().getHead(eType, textureKey);
		}
		else{
			OfflinePlayer p = pl.getServer().getOfflinePlayer(target);
			if(p.hasPlayedBefore() || WebUtils.checkExists(p.getName())){
				head = HeadUtils.getPlayerHead(p);
			}
			else if(target.startsWith("MHF_") && HeadUtils.MHF_Lookup.containsKey(target.toUpperCase())){
				head = new ItemStack(Material.PLAYER_HEAD);
				SkullMeta meta = (SkullMeta) head.getItemMeta();
				meta.setOwner(target);
				meta.setDisplayName(ChatColor.YELLOW+HeadUtils.MHF_Lookup.get(target.toUpperCase()));
				head.setItemMeta(meta);
			}
			else if(target.length() > 100){
				head = HeadUtils.makeSkull(target, ChatColor.YELLOW+"UNKNOWN Head");
			}
		}
		if(head != null){
			String headName = head.hasItemMeta() && head.getItemMeta().hasDisplayName()
					? head.getItemMeta().getDisplayName() : nameFromType(head.getType());
			((Player)sender).getInventory().addItem(head);
			sender.sendMessage(ChatColor.GREEN+"Spawned Head: " + ChatColor.YELLOW + headName);
		}
		else sender.sendMessage(ChatColor.RED+"Head \""+target+"\" not found");
		return true;
	}
}