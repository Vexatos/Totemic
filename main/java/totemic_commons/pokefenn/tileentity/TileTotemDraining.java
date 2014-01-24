package totemic_commons.pokefenn.tileentity;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.IPlantable;
import totemic_commons.pokefenn.ModItems;
import totemic_commons.pokefenn.client.ParticleUtil;
import totemic_commons.pokefenn.configuration.ConfigurationSettings;
import totemic_commons.pokefenn.lib.Particles;
import totemic_commons.pokefenn.lib.Strings;
import totemic_commons.pokefenn.network.PacketSpawnParticle;

import java.util.Random;

public class TileTotemDraining extends TileTotemic implements IInventory
{

    private Random rand = new Random();

    private ItemStack[] inventory;

    public static final int INVENTORY_SIZE = 1;

    public static final int SLOT_ONE = 0;

    protected boolean hasDoneEffect;

    protected int totemRadius = ConfigurationSettings.TOTEM_DRAINING_RANGE;

    public TileTotemDraining()
    {

        inventory = new ItemStack[INVENTORY_SIZE];

    }


    @Override
    public int getSizeInventory()
    {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex)
    {
        return inventory[slotIndex];


    }

    @Override
    public ItemStack decrStackSize(int slotIndex, int decrementAmount)
    {

        ItemStack itemStack = getStackInSlot(slotIndex);
        if (itemStack != null)
        {
            if (itemStack.stackSize <= decrementAmount)
            {
                setInventorySlotContents(slotIndex, null);
            } else
            {
                itemStack = itemStack.splitStack(decrementAmount);
                if (itemStack.stackSize == 0)
                {
                    setInventorySlotContents(slotIndex, null);
                }
            }
        }

        return itemStack;
    }


    @Override
    public ItemStack getStackInSlotOnClosing(int slotIndex)
    {

        ItemStack itemStack = getStackInSlot(slotIndex);
        if (itemStack != null)
        {
            setInventorySlotContents(slotIndex, null);
        }
        return itemStack;
    }

    @Override
    public void setInventorySlotContents(int slotIndex, ItemStack itemStack)
    {

        inventory[slotIndex] = itemStack;
        if (itemStack != null && itemStack.stackSize > getInventoryStackLimit())
        {
            itemStack.stackSize = getInventoryStackLimit();
        }


    }

    @Override
    public String getInvName()
    {
        return Strings.CONTAINER_TOTEM_DRAINING_NAME;
    }

    @Override
    public boolean isInvNameLocalized()
    {
        return true;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 1;
    }

    @Override
    public void openChest()
    {

    }

    @Override
    public void closeChest()
    {

    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack)
    {
        if (itemStack.itemID == ModItems.chlorophyllCrystal.itemID && i == SLOT_ONE && !this.worldObj.isRemote)
        {
            this.setInventorySlotContents(i, itemStack);

            return true;
        } else
        {
            return false;
        }

    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound)
    {

        super.readFromNBT(nbtTagCompound);

        // Read in the ItemStacks in the inventory from NBT
        NBTTagList tagList = nbtTagCompound.getTagList("Items");
        inventory = new ItemStack[this.getSizeInventory()];
        for (int i = 0; i < tagList.tagCount(); ++i)
        {
            NBTTagCompound tagCompound = (NBTTagCompound) tagList.tagAt(i);
            byte slotIndex = tagCompound.getByte("Slot");
            if (slotIndex >= 0 && slotIndex < inventory.length)
            {
                inventory[slotIndex] = ItemStack.loadItemStackFromNBT(tagCompound);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtTagCompound)
    {

        super.writeToNBT(nbtTagCompound);

        // Write the ItemStacks in the inventory to NBT
        NBTTagList tagList = new NBTTagList();
        for (int currentIndex = 0; currentIndex < inventory.length; ++currentIndex)
        {
            if (inventory[currentIndex] != null)
            {
                NBTTagCompound tagCompound = new NBTTagCompound();
                tagCompound.setByte("Slot", (byte) currentIndex);
                inventory[currentIndex].writeToNBT(tagCompound);
                tagList.appendTag(tagCompound);
            }
        }
        nbtTagCompound.setTag("Items", tagList);
    }


    public void updateEntity()
    {
        if (this.worldObj.getTotalWorldTime() % 100L == 0L)
        {
            this.drainEffect();
            super.updateEntity();
        }

    }

    protected void handleChlorophyllCrystal()
    {
        if (this.getStackInSlot(SLOT_ONE) != null && this.getStackInSlot(SLOT_ONE).itemID == ModItems.chlorophyllCrystal.itemID && this.getStackInSlot(SLOT_ONE).getItemDamage() < 500 && rand.nextBoolean())
        {
            this.getStackInSlot(SLOT_ONE).setItemDamage(this.getStackInSlot(SLOT_ONE).getItemDamage() - 1);

        }

    }

    protected void drainEffect()
    {

        this.loopThroughArea();

    }

    protected void reducePlantMetadata(int x, int y, int z)
    {
        //yCoords is there because the totem has to be on the same level as the IPlantable's
        Block blockQuery = Block.blocksList[this.worldObj.getBlockId(x, y, z)];

        if (this.worldObj.getBlockMetadata(x, y, z) >= 6 && blockQuery instanceof IPlantable && this.getStackInSlot(SLOT_ONE) != null && rand.nextBoolean())
        {
            if (this.getStackInSlot(SLOT_ONE).itemID == ModItems.chlorophyllCrystal.itemID)
            {
                ParticleUtil.spawnParticle(this.worldObj, Particles.ESSENCE_DRAIN, x, y, z, 10, 10, 10);
                new PacketSpawnParticle(Particles.ESSENCE_DRAIN, (double) x, (double) y, (double) z, 8, 10, 8);
                this.worldObj.setBlockMetadataWithNotify(x, y, z, this.worldObj.getBlockMetadata(x, y, z) - 1, 2);
                this.handleChlorophyllCrystal();
            }
        }
    }

    protected void loopThroughArea()
    {

        for (int i = -totemRadius; i <= totemRadius; i++)
        {
            for (int j = -totemRadius; j <= totemRadius; j++)
            {
                reducePlantMetadata(xCoord + i, yCoord, zCoord + j);
            }
        }
    }


    public boolean canUpdate()
    {
        return true;
    }


}