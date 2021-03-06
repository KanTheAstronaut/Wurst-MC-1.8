/*
 * Copyright � 2014 - 2017 | Wurst-Imperium | All rights reserved.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.features.mods;

import java.util.HashSet;
import java.util.LinkedList;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerDigging.Action;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import tk.wurst_client.events.LeftClickEvent;
import tk.wurst_client.events.listeners.LeftClickListener;
import tk.wurst_client.events.listeners.RenderListener;
import tk.wurst_client.events.listeners.UpdateListener;
import tk.wurst_client.features.Feature;
import tk.wurst_client.utils.BlockUtils;
import tk.wurst_client.utils.RenderUtils;

@Mod.Info(
	description = "Slower Nuker that bypasses any cheat prevention\n"
		+ "PlugIn. Not required on most NoCheat+ servers!",
	name = "NukerLegit",
	tags = "LegitNuker, nuker legit, legit nuker",
	help = "Mods/NukerLegit")
@Mod.Bypasses
public class NukerLegitMod extends Mod
	implements LeftClickListener, RenderListener, UpdateListener
{
	private static Block currentBlock;
	private float currentDamage;
	private EnumFacing side = EnumFacing.UP;
	private byte blockHitDelay = 0;
	private BlockPos pos;
	private boolean shouldRenderESP;
	private int oldSlot = -1;
	
	@Override
	public String getRenderName()
	{
		return wurst.mods.nukerMod.getRenderName() + "Legit";
	}
	
	@Override
	public Feature[] getSeeAlso()
	{
		return new Feature[]{wurst.mods.nukerMod, wurst.mods.speedNukerMod,
			wurst.mods.tunnellerMod, wurst.mods.fastBreakMod,
			wurst.mods.autoMineMod};
	}
	
	@Override
	public void onEnable()
	{
		if(wurst.mods.nukerMod.isEnabled())
			wurst.mods.nukerMod.setEnabled(false);
		if(wurst.mods.speedNukerMod.isEnabled())
			wurst.mods.speedNukerMod.setEnabled(false);
		if(wurst.mods.tunnellerMod.isEnabled())
			wurst.mods.tunnellerMod.setEnabled(false);
		wurst.events.add(LeftClickListener.class, this);
		wurst.events.add(UpdateListener.class, this);
		wurst.events.add(RenderListener.class, this);
	}
	
	@Override
	public void onRender()
	{
		if(blockHitDelay == 0 && shouldRenderESP)
			if(!mc.player.capabilities.isCreativeMode && currentBlock
				.getPlayerRelativeBlockHardness(mc.player, mc.world, pos) < 1)
				RenderUtils.nukerBox(pos, currentDamage);
			else
				RenderUtils.nukerBox(pos, 1);
	}
	
	@Override
	public void onUpdate()
	{
		shouldRenderESP = false;
		BlockPos newPos = find();
		if(newPos == null)
		{
			if(oldSlot != -1)
			{
				mc.player.inventory.currentItem = oldSlot;
				oldSlot = -1;
			}
			return;
		}
		if(pos == null || !pos.equals(newPos))
			currentDamage = 0;
		pos = newPos;
		currentBlock = mc.world.getBlockState(pos).getBlock();
		if(blockHitDelay > 0)
		{
			blockHitDelay--;
			return;
		}
		BlockUtils.faceBlockClient(pos);
		if(currentDamage == 0)
		{
			mc.player.connection.sendPacket(new CPacketPlayerDigging(
				Action.START_DESTROY_BLOCK, pos, side));
			if(wurst.mods.autoToolMod.isActive() && oldSlot == -1)
				oldSlot = mc.player.inventory.currentItem;
			if(mc.player.capabilities.isCreativeMode || currentBlock
				.getPlayerRelativeBlockHardness(mc.player, mc.world, pos) >= 1)
			{
				currentDamage = 0;
				shouldRenderESP = true;
				mc.player.swingArm();
				mc.playerController.onPlayerDestroyBlock(pos, side);
				blockHitDelay = (byte)4;
				return;
			}
		}
		wurst.mods.autoToolMod.setSlot(pos);
		mc.player.connection.sendPacket(new CPacketAnimation());
		shouldRenderESP = true;
		currentDamage += currentBlock.getPlayerRelativeBlockHardness(mc.player,
			mc.world, pos);
		mc.world.sendBlockBreakProgress(mc.player.getEntityId(), pos,
			(int)(currentDamage * 10.0F) - 1);
		if(currentDamage >= 1)
		{
			mc.player.connection.sendPacket(
				new CPacketPlayerDigging(Action.STOP_DESTROY_BLOCK, pos, side));
			mc.playerController.onPlayerDestroyBlock(pos, side);
			blockHitDelay = (byte)4;
			currentDamage = 0;
		}
	}
	
	@Override
	public void onDisable()
	{
		wurst.events.remove(LeftClickListener.class, this);
		wurst.events.remove(UpdateListener.class, this);
		wurst.events.remove(RenderListener.class, this);
		if(oldSlot != -1)
		{
			mc.player.inventory.currentItem = oldSlot;
			oldSlot = -1;
		}
		currentDamage = 0;
		shouldRenderESP = false;
		wurst.mods.nukerMod.id = 0;
		wurst.files.saveOptions();
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(mc.objectMouseOver == null
			|| mc.objectMouseOver.getBlockPos() == null)
			return;
		if(wurst.mods.nukerMod.mode.getSelected() == 1
			&& mc.world.getBlockState(mc.objectMouseOver.getBlockPos())
				.getBlock().getMaterial() != Material.AIR)
		{
			wurst.mods.nukerMod.id = Block.getIdFromBlock(mc.world
				.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock());
			wurst.files.saveOptions();
		}
	}
	
	private BlockPos find()
	{
		LinkedList<BlockPos> queue = new LinkedList<BlockPos>();
		HashSet<BlockPos> alreadyProcessed = new HashSet<BlockPos>();
		queue.add(new BlockPos(mc.player));
		while(!queue.isEmpty())
		{
			BlockPos currentPos = queue.poll();
			if(alreadyProcessed.contains(currentPos))
				continue;
			alreadyProcessed.add(currentPos);
			if(BlockUtils.getPlayerBlockDistance(currentPos) > Math
				.min(wurst.mods.nukerMod.range.getValueF(), 4.25F))
				continue;
			int currentID = Block
				.getIdFromBlock(mc.world.getBlockState(currentPos).getBlock());
			if(currentID != 0)
				switch(wurst.mods.nukerMod.mode.getSelected())
				{
					case 1:
						if(currentID == wurst.mods.nukerMod.id)
							return currentPos;
						break;
					case 2:
						if(currentPos.getY() >= mc.player.posY)
							return currentPos;
						break;
					case 3:
						if(mc.world.getBlockState(currentPos).getBlock()
							.getPlayerRelativeBlockHardness(mc.player, mc.world,
								currentPos) >= 1)
							return currentPos;
						break;
					default:
						return currentPos;
				}
			if(!mc.world.getBlockState(currentPos).getBlock().getMaterial()
				.blocksMovement())
			{
				queue.add(currentPos.add(0, 0, -1));// north
				queue.add(currentPos.add(0, 0, 1));// south
				queue.add(currentPos.add(-1, 0, 0));// west
				queue.add(currentPos.add(1, 0, 0));// east
				queue.add(currentPos.add(0, -1, 0));// down
				queue.add(currentPos.add(0, 1, 0));// up
			}
		}
		return null;
		
	}
}
