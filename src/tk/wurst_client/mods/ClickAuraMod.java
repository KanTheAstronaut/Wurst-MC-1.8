/*
 * Copyright � 2014 - 2016 | Wurst-Imperium | All rights reserved.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.mods;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import tk.wurst_client.events.listeners.UpdateListener;
import tk.wurst_client.navigator.NavigatorItem;
import tk.wurst_client.utils.EntityUtils;

@Mod.Info(
	description = "Automatically attacks the closest valid entity whenever you\n"
		+ "click.\n"
		+ "Warning: ClickAuras generally look more suspicious than Killauras\n"
		+ "and are easier to detect. It is recommended to use Killaura or\n"
		+ "TriggerBot instead.",
	name = "ClickAura",
	tags = "Click Aura,ClickAimbot,Click Aimbot",
	help = "Mods/ClickAura")
public class ClickAuraMod extends Mod implements UpdateListener
{
	@Override
	public NavigatorItem[] getSeeAlso()
	{
		return new NavigatorItem[]{wurst.special.targetSpf,
			wurst.mods.killauraMod, wurst.mods.killauraLegitMod,
			wurst.mods.multiAuraMod, wurst.mods.triggerBotMod};
	}
	
	@Override
	public void onEnable()
	{
		// TODO: Clean up this mess!
		if(wurst.mods.killauraMod.isEnabled())
			wurst.mods.killauraMod.setEnabled(false);
		if(wurst.mods.killauraLegitMod.isEnabled())
			wurst.mods.killauraLegitMod.setEnabled(false);
		if(wurst.mods.multiAuraMod.isEnabled())
			wurst.mods.multiAuraMod.setEnabled(false);
		if(wurst.mods.triggerBotMod.isEnabled())
			wurst.mods.triggerBotMod.setEnabled(false);
		wurst.events.add(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		updateMS();
		EntityLivingBase en = EntityUtils.getClosestEntity(true, true);
		if(en == null
			|| mc.player.getDistanceToEntity(en) > wurst.mods.killauraMod.realRange)
		{
			EntityUtils.lookChanged = false;
			return;
		}
		EntityUtils.lookChanged = true;
		if(hasTimePassedS(wurst.mods.killauraMod.realSpeed)
			&& mc.gameSettings.keyBindAttack.pressed)
		{
			if(wurst.mods.autoSwordMod.isActive())
				AutoSwordMod.setSlot();
			wurst.mods.criticalsMod.doCritical();
			wurst.mods.blockHitMod.doBlock();
			if(EntityUtils.faceEntityPacket(en))
			{
				mc.player.swingItem();
				mc.player.sendQueue.addToSendQueue(new C02PacketUseEntity(
					en, C02PacketUseEntity.Action.ATTACK));
			}
			updateLastMS();
		}
	}
	
	@Override
	public void onDisable()
	{
		wurst.events.remove(UpdateListener.class, this);
		EntityUtils.lookChanged = false;
	}
}