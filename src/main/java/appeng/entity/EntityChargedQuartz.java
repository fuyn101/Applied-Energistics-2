/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.entity;


import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import appeng.api.definitions.IMaterials;
import appeng.client.EffectType;
import appeng.core.AEConfig;
import appeng.core.Api;
import appeng.core.AppEng;
import appeng.api.features.AEFeature;
import appeng.util.Platform;


public final class EntityChargedQuartz extends AEBaseEntityItem
{

	private int delay = 0;
	private int transformTime = 0;

	public EntityChargedQuartz(EntityType<? extends EntityChargedQuartz> entityType, World world) {
		super(entityType, world);
	}

	public EntityChargedQuartz(final World w, final double x, final double y, final double z, final ItemStack is )
	{
		super( w, x, y, z, is );
	}

	@Override
	public void tick()
	{
		super.tick();

		if( this.removed || !AEConfig.instance().isFeatureEnabled( AEFeature.IN_WORLD_FLUIX ) )
		{
			return;
		}

		if( world.isRemote && this.delay > 30 && AEConfig.instance().isEnableEffects() )
		{
			AppEng.proxy.spawnEffect( EffectType.Lightning, this.world, this.getPosX(), this.getPosY(), this.getPosZ(), null );
			this.delay = 0;
		}

		this.delay++;

		final int j = MathHelper.floor( this.getPosX() );
		final int i = MathHelper.floor( ( this.getBoundingBox().minY + this.getBoundingBox().maxY ) / 2.0D );
		final int k = MathHelper.floor( this.getPosZ() );

		BlockState state = this.world.getBlockState( new BlockPos( j, i, k ) );
		final Material mat = state.getMaterial();

		if( Platform.isServer() && mat.isLiquid() )
		{
			this.transformTime++;
			if( this.transformTime > 60 )
			{
				if( !this.transform() )
				{
					this.transformTime = 0;
				}
			}
		}
		else
		{
			this.transformTime = 0;
		}
	}

	private boolean transform()
	{
		final ItemStack item = this.getItem();
		final IMaterials materials = Api.INSTANCE.definitions().materials();

		if( materials.certusQuartzCrystalCharged().isSameAs( item ) )
		{
			final AxisAlignedBB region = new AxisAlignedBB( this.getPosX() - 1, this.getPosY() - 1, this
					.getPosZ() - 1, this.getPosX() + 1, this.getPosY() + 1, this.getPosZ() + 1 );
			final List<Entity> l = this.getCheckedEntitiesWithinAABBExcludingEntity( region );

			ItemEntity redstone = null;
			ItemEntity netherQuartz = null;

			for( final Entity e : l )
			{
				if( e instanceof ItemEntity && !e.removed )
				{
					final ItemStack other = ( (ItemEntity) e ).getItem();
					if( !other.isEmpty() )
					{
						if( ItemStack.areItemsEqual( other, new ItemStack( Items.REDSTONE ) ) )
						{
							redstone = (ItemEntity) e;
						}

						if( ItemStack.areItemsEqual( other, new ItemStack( Items.QUARTZ ) ) )
						{
							netherQuartz = (ItemEntity) e;
						}
					}
				}
			}

			if( redstone != null && netherQuartz != null )
			{
				this.getItem().grow( -1 );
				redstone.getItem().grow( -1 );
				netherQuartz.getItem().grow( -1 );

				if( this.getItem().getCount() <= 0 )
				{
					this.remove();
				}

				if( redstone.getItem().getCount() <= 0 )
				{
					redstone.remove();
				}

				if( netherQuartz.getItem().getCount() <= 0 )
				{
					netherQuartz.remove();
				}

				materials.fluixCrystal().maybeStack( 2 ).ifPresent( is ->
				{
					final ItemEntity entity = new ItemEntity( this.world, this.getPosX(), this.getPosY(), this.getPosZ(), is );

					this.world.addEntity( entity );
				} );

				return true;
			}
		}

		return false;
	}
}
