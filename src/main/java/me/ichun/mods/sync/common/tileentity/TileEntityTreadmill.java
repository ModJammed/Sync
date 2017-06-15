package me.ichun.mods.sync.common.tileentity;

import me.ichun.mods.sync.common.Sync;
import me.ichun.mods.sync.common.block.BlockDualVertical;
import me.ichun.mods.sync.common.block.EnumType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;

//@Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHCore")
public class TileEntityTreadmill extends TileEntity implements ITickable//, IEnergyHandler TODO
{
	public TileEntityTreadmill pair;
	
	public boolean back;
	
	public EnumFacing face;
	
	public EntityLiving latchedEnt;
	public int latchedEntId;
	
	public float latchedHealth;
	public int timeRunning;
	
	public boolean resync;

	public TileEntityTreadmill()
	{
		pair = null;
		back = false;
		latchedEnt = null;
		
		face = EnumFacing.SOUTH;
		
		resync = false;
	}
	
	@Override
	public void update()
	{
		if(resync)
		{
			TileEntity te = world.getTileEntity(pos.offset(back ? face.getOpposite() : face));
			if(te != null && te.getClass() == this.getClass())
			{
				TileEntityTreadmill sc = (TileEntityTreadmill)te;
				sc.pair = this;
				pair = sc;
			}
			if(latchedEntId != -1)
			{
				if(world.isRemote)
				{
					Entity ent = world.getEntityByID(latchedEntId);
					if(ent != null && ent.getDistance(getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1)) < 7D)
					{
						latchedEnt = (EntityLiving)ent;
						latchedHealth = latchedEnt.getHealth();
					}
				}
				else
				{
					AxisAlignedBB aabb = new AxisAlignedBB(getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1), getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1)).expand(0.4D, 0.4D, 0.4D);
					List list = world.getEntitiesWithinAABB(Entity.class, aabb);

                    for (Object aList : list) {
                        Entity ent = (Entity) aList;

                        if (isEntityValidForTreadmill(ent)) {
                            if (ent.posX > aabb.minX && ent.posX < aabb.maxX && ent.posY > aabb.minY && ent.posY < aabb.maxY && ent.posZ > aabb.minZ && ent.posZ < aabb.maxZ) {
                                latchedEnt = (EntityLiving) ent;
                                latchedHealth = latchedEnt.getHealth();
                                latchedEnt.setLocationAndAngles(getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1), face.getOpposite().getHorizontalAngle(), 0.0F);
								IBlockState state = world.getBlockState(pos);
                                world.notifyBlockUpdate(pos, state, state, 3);
                                break;
                            }
                        }
                    }
				}
			}
			else if(latchedEnt != null)
			{
				latchedEnt = null;
				timeRunning = 0;
				IBlockState state = world.getBlockState(pos);
				world.notifyBlockUpdate(pos, state ,state, 3);
			}
		}
		resync = false;
		
		if(world.isRemote && !back)
		{
			if(latchedEnt == null && latchedEntId != -1 && world.getWorldTime() % 27L == 0L)
			{
				Entity ent = world.getEntityByID(latchedEntId);
				if(ent != null && ent.getDistance(getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1)) < 3D)
				{
					latchedEnt = (EntityLiving)ent;
					latchedHealth = latchedEnt.getHealth();
				}
			}
			if(latchedEnt != null && latchedEnt.isDead)
			{
				Entity ent = world.getEntityByID(latchedEntId);
				if(ent != null && ent.getDistance(getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1)) < 7D)
				{
					latchedEnt = (EntityLiving)ent;
					latchedHealth = latchedEnt.getHealth();
				}
			}
			if(latchedEnt != null)
			{
				latchedEnt.setLocationAndAngles(getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1), face.getOpposite().getHorizontalAngle(), 0.0F);
				timeRunning++;
				if(timeRunning > 12000)
				{
					timeRunning = 12000;
				}
				
				if(0.3F + (MathHelper.clamp((float)timeRunning / 12000F, 0.0F, 1.0F) * 0.7F) > world.rand.nextFloat())
				{
					spawnParticles();
				}
			}
		}
		if(!world.isRemote && !back)
		{
			AxisAlignedBB aabb = latchedEnt != null ? latchedEnt.getEntityBoundingBox().contract(0.1D) : new AxisAlignedBB(getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1), getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1)).expand(0.15D, 0.005D, 0.15D);
			List list = world.getEntitiesWithinAABB(Entity.class, aabb);
	
			if(latchedEnt != null)
			{
				boolean remove = false;
				if(latchedEnt instanceof EntityTameable)
				{
					EntityTameable entityTameable = (EntityTameable)latchedEnt;
					//Remove sitting entities
					if(entityTameable.isSitting())
					{
						timeRunning = 0;
						IBlockState state = world.getBlockState(pos);
						world.notifyBlockUpdate(pos, state ,state, 3);
						
						remove = true;
					}
					if(entityTameable.isTamed())
					{
						latchedEnt.setLocationAndAngles(getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1), face.getOpposite().getHorizontalAngle(), 0.0F);
						
						aabb = latchedEnt.getEntityBoundingBox().contract(0.1D);
						list = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb);
					}
					else
					{
						entityTameable.ticksExisted = 1200; //anti despawn methods
					}
				}
				for (Object aList : list) {
					Entity ent = (Entity) aList;

					if (ent != latchedEnt && ent instanceof EntityLivingBase && !(ent instanceof EntityPlayer)) {
						double velo = 0.9D;
						switch (face) {
							case SOUTH: {
								ent.motionZ = velo;
								break;
							}
							case WEST: {
								ent.motionX = -velo;
								break;
							}
							case NORTH: {
								ent.motionZ = -velo;
								break;
							}
							case EAST: {
								ent.motionX = velo;
								break;
							}
						}

						remove = true;
					}
				}
				if(latchedEnt != null && (!list.contains(latchedEnt) || remove || latchedHealth > latchedEnt.getHealth()))
				{
					if(latchedHealth <= latchedEnt.getHealth())
					{
						double velo = 1.3D;
						switch(face)
						{
							case SOUTH:
							{
								latchedEnt.motionZ = velo;
								break;
							}
							case WEST:
							{
								latchedEnt.motionX = -velo;
								break;
							}
							case NORTH:
							{
								latchedEnt.motionZ = -velo;
								break;
							}
							case EAST:
							{
								latchedEnt.motionX = velo;
								break;
							}
						}
					}
					latchedEnt = null;
					timeRunning = 0;
					IBlockState state = world.getBlockState(pos);
					world.notifyBlockUpdate(pos, state ,state, 3);
				}
				if(latchedEnt != null)
				{
					latchedHealth = latchedEnt.getHealth();
					latchedEnt.setLocationAndAngles(getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1), face.getOpposite().getHorizontalAngle(), 0.0F);
					latchedEnt.getNavigator().clearPathEntity();
					if (timeRunning < 12000) {
						timeRunning++;
					}
					
					//Still running. This sends RF power to nearby IEnergyHandlers
//                    if (Sync.hasCoFHCore) { TODO Energy
//                        this.sendRFEnergyToNearbyDevices();
//                    }
				}
			}
			else
			{
				for (Object aList : list) {
					Entity ent = (Entity) aList;

					if (TileEntityTreadmill.isEntityValidForTreadmill(ent)) {
						if (ent.posX > aabb.minX && ent.posX < aabb.maxX && ent.posY > aabb.minY && ent.posY < aabb.maxY && ent.posZ > aabb.minZ && ent.posZ < aabb.maxZ) {
							latchedEnt = (EntityLiving) ent;
							latchedHealth = latchedEnt.getHealth();
							timeRunning = 0;
							latchedEnt.setLocationAndAngles(getMidCoord(0), pos.getY() + 0.175D, getMidCoord(1), face.getOpposite().getHorizontalAngle(), 0.0F);
							IBlockState state = world.getBlockState(pos);
							world.notifyBlockUpdate(pos, state, state.withProperty(BlockDualVertical.TYPE, EnumType.TREADMILL), 3);
							break;
						}
					}
				}
			}
		}
	}

//    @Optional.Method(modid = "CoFHCore") TODO Energy
//    private void sendRFEnergyToNearbyDevices() {
//        float power = powerOutput() / (float)Sync.config.ratioRF; //2PW = 1RF
//        int handlerCount = 0;
//        IEnergyHandler[] handlers = new IEnergyHandler[ForgeDirection.VALID_DIRECTIONS.length];
//        for(ForgeDirection dir:ForgeDirection.VALID_DIRECTIONS)
//        {
//            if(dir == ForgeDirection.UP)
//            {
//                continue;
//            }
//            TileEntity te = world.getTileEntity(xCoord + dir.offsetX, pos.getY() + dir.offsetY, zCoord + dir.offsetZ);
//            if(te instanceof IEnergyHandler && !(te instanceof TileEntityDualVertical))
//            {
//                IEnergyHandler energy = (IEnergyHandler) te;
//                if(energy.canConnectEnergy(dir.getOpposite()))
//                {
//                    handlerCount++;
//                    //Test if they can recieve power via simulate
//                    if(energy.receiveEnergy(dir.getOpposite(), (int)power, true) > 0)
//                    {
//                        handlers[dir.getOpposite().ordinal()] = energy;
//                    }
//                }
//            }
//        }
//        for(int i = 0; i < handlers.length; i++)
//        {
//            IEnergyHandler handler = handlers[i];
//            if(handler != null)
//            {
//                //Sends power equally to all nearby IEnergyHandlers that can receive it
//                handler.receiveEnergy(ForgeDirection.getOrientation(i), Math.max(Math.round(power / (float)handlerCount), 1), false);
//            }
//        }
//    }
	
	@SideOnly(Side.CLIENT)
	public void spawnParticles() 
	{
//		if(latchedEnt != null && pair != null) TODO figure this out
//		{
//			double xVelo = (face == 1 ? -30D : face == 3 ? 30.0D : 0.0D);
//			double zVelo = face == 0 ? 30D : face == 2 ? -30D : 0.0D;
//			if(world.rand.nextFloat() < 0.5F)
//			{
//				Minecraft.getMinecraft().effectRenderer.addEffect((new EntityDiggingFX(worldObj, pair.xCoord + worldObj.rand.nextFloat(), pair.pos.getY() + 0.4D, pair.zCoord + worldObj.rand.nextFloat(), xVelo, 0.0D, zVelo, Sync.blockDualVertical, 2)).applyRenderColor(2));
//			}
//			else
//			{
//				Minecraft.getMinecraft().effectRenderer.addEffect((new EntityDiggingFX(worldObj, xCoord + worldObj.rand.nextFloat(), pos.getY() + 0.4D, zCoord + worldObj.rand.nextFloat(), xVelo, 0.0D, zVelo, Sync.blockDualVertical, 2)).applyRenderColor(2));
//			}
//
//			if(timeRunning == 12000 && world.rand.nextFloat() < 0.2F)
//			{
//				xVelo *= 0.01D;
//				zVelo *= 0.01D;
//				Minecraft.getMinecraft().effectRenderer.addEffect(new EntitySmokeFX(worldObj, xCoord + worldObj.rand.nextFloat(), pos.getY() + 0.4D, zCoord + worldObj.rand.nextFloat(), xVelo, 0.0D, zVelo));
//			}
//		}
	}

	public double getMidCoord(int i)
	{
		if(back && pair != null)
		{
			return pair.getMidCoord(i);
		}
		if(i == 0)//x coord
		{
			return (face == EnumFacing.WEST ? pos.getX() : face == EnumFacing.EAST ? pos.getX() + 1 : pos.getX() + 0.5D);
		}
		else //z coord
		{
			return (face == EnumFacing.SOUTH ? pos.getZ() + 1 : face == EnumFacing.NORTH ? pos.getZ() : pos.getZ() + 0.5D);
		}
	}
	
	public float powerOutput()
	{
		if(back && pair != null)
		{
			return pair.powerOutput();
		}
		float power = 0.0F;
		if(latchedEnt != null)
		{
			power = Sync.TREADMILL_ENTITY_HASH_MAP.get(latchedEnt.getClass());
			if (latchedEnt instanceof EntityTameable && ((EntityTameable) latchedEnt).isTamed()) power = (power / 2) + (power / 4); //Decrease power if the entity isn't tamed
			power += MathHelper.clamp((float)timeRunning / 12000F, 0.0F, 1.0F) * 2F;
		}
		return power;
	}
	
	public void setup(TileEntityTreadmill sc, boolean b, EnumFacing face2)
	{
		pair = sc;
		back = b;
		face = face2;
	}
	
	@Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
	{
		readFromNBT(pkt.getNbtCompound());
	}

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag = writeToNBT(tag);
        return new SPacketUpdateTileEntity(pos, 0, tag);
    }

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	@Nonnull
	public NBTTagCompound writeToNBT(NBTTagCompound tag)
	{
		tag = super.writeToNBT(tag);
		tag.setBoolean("back", back);
		tag.setInteger("face", face.getIndex());
		tag.setInteger("latchedID", latchedEnt != null ? latchedEnt.getEntityId() : -1);
		tag.setInteger("timeRunning", timeRunning);
		return tag;
	}
	 
	@Override
	public void readFromNBT(NBTTagCompound tag)
	{
		super.readFromNBT(tag);
		back = tag.getBoolean("back");
		face = EnumFacing.getFront(tag.getInteger("face"));
		latchedEntId = tag.getInteger("latchedID");
		timeRunning = tag.getInteger("timeRunning");
		
		resync = true;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		return new AxisAlignedBB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 1, pos.getZ() + 2);
	}

	@Override
	public Block getBlockType()
	{
		return Sync.blockDualVertical;
	}

	//Will return true if the entity can use the treadmill
	public static boolean isEntityValidForTreadmill(Entity entity) {
		return Sync.TREADMILL_ENTITY_HASH_MAP.containsKey(entity.getClass()) && !((EntityLiving) entity).isChild() && !(entity instanceof EntityTameable && ((EntityTameable) entity).isSitting());
	}
	
//	// TE methods
//	@Override TODO ENERGY
//	@Optional.Method(modid = "CoFHCore")
//	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
//	{
//		return 0;
//	}
//
//	@Override
//	@Optional.Method(modid = "CoFHCore")
//	public int extractEnergy(ForgeDirection from, int maxExtract, boolean doExtract)
//	{
//		return 0;
//	}
//
//	@Override
//	@Optional.Method(modid = "CoFHCore")
//	public boolean canConnectEnergy(ForgeDirection from)
//	{
//		return !back;
//	}
//
//	@Override
//	@Optional.Method(modid = "CoFHCore")
//	public int getEnergyStored(ForgeDirection from)
//	{
//		return 0;
//	}
//
//	@Override
//	@Optional.Method(modid = "CoFHCore")
//	public int getMaxEnergyStored(ForgeDirection from)
//	{
//		return 0;
//	}
}
