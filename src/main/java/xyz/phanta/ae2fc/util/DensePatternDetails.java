package xyz.phanta.ae2fc.util;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// ae2 api docs say the inputs/ouputs arrays can contain nulls, but their use of the api directly contradicts that
// so we'll just go by behaviour rather than what the docs say and filter out nulls
public class DensePatternDetails implements ICraftingPatternDetails, Comparable<DensePatternDetails> {

    private final ItemStack patternStack;
    private IAEItemStack patternStackAe;
    @Nullable
    private IAEItemStack[] inputs = null, inputsCond = null, outputs = null, outputsCond = null;
    private int priority = 0;

    public DensePatternDetails(ItemStack stack) {
        this.patternStack = stack;
        this.patternStackAe = Objects.requireNonNull(AEItemStack.fromItemStack(stack)); // s2g
    }

    @Override
    public ItemStack getPattern() {
        return patternStack;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean isCraftable() {
        return false;
    }

    @Override
    public boolean canSubstitute() {
        return false;
    }

    @Override
    public IAEItemStack[] getInputs() {
        return checkInitialized(inputs);
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return checkInitialized(inputsCond);
    }

    public boolean setInputs(IAEItemStack[] inputs) {
        for (IAEItemStack stack : inputs) { // see note at top of class
            if (stack == null) {
                return false;
            }
        }
        IAEItemStack[] condensed = condenseStacks(inputs);
        if (condensed.length == 0) {
            return false;
        }
        this.inputs = inputs;
        this.inputsCond = condensed;
        return true;
    }

    @Override
    public IAEItemStack[] getOutputs() {
        return checkInitialized(outputs);
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return checkInitialized(outputsCond);
    }

    public boolean setOutputs(IAEItemStack[] outputs) {
        for (IAEItemStack stack : outputs) { // see note at top of class
            if (stack == null) {
                return false;
            }
        }
        IAEItemStack[] condensed = condenseStacks(outputs);
        if (condensed.length == 0) {
            return false;
        }
        this.outputs = outputs;
        this.outputsCond = condensed;
        return true;
    }

    private static IAEItemStack[] condenseStacks(IAEItemStack[] stacks) {
        // AE item stacks are equivalent iff they are of the same item type (not accounting for stack size)
        // thus, it's not the semantically-correct definition of "equal" but it's useful for matching item types
        Map<IAEItemStack, IAEItemStack> accMap = new HashMap<>();
        for (IAEItemStack stack : stacks) {
            if (stack != null) {
                IAEItemStack acc = accMap.get(stack);
                if (acc == null) {
                    accMap.put(stack, stack.copy());
                } else {
                    acc.add(stack);
                }
            }
        }
        return accMap.values().toArray(new IAEItemStack[0]);
    }

    @Override
    public ItemStack getOutput(InventoryCrafting craftingInv, World world) {
        throw new IllegalStateException("Not a crafting recipe!");
    }

    @Override
    public boolean isValidItemForSlot(int slotIndex, ItemStack itemStack, World world) {
        throw new IllegalStateException("Not a crafting recipe!");
    }

    private static <T> T checkInitialized(@Nullable T value) {
        if (value == null) {
            throw new IllegalStateException("Pattern is not initialized!");
        }
        return value;
    }

    @Override
    public int hashCode() {
        return patternStackAe.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // ae2 null-checks the pattern stack here for some reason, but doesn't null-check in hashCode()
        // this is inconsistent, so i've just decided to assert non-null in the constructor, which is to say that
        // the pattern stack can never be null here
        return obj instanceof DensePatternDetails && patternStackAe.equals(((DensePatternDetails)obj).patternStackAe);
    }

    @Override
    public int compareTo(DensePatternDetails o) {
        return Integer.compare(o.priority, this.priority);
    }

    public ItemStack writeToStack() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Inputs", writeStackArray(checkInitialized(inputs)));
        tag.setTag("Outputs", writeStackArray(checkInitialized(outputs)));
        patternStack.setTagCompound(tag);
        patternStackAe = Objects.requireNonNull(AEItemStack.fromItemStack(patternStack));
        return patternStack;
    }

    private static NBTTagList writeStackArray(IAEItemStack[] stacks) {
        NBTTagList listTag = new NBTTagList();
        for (IAEItemStack stack : stacks) {
            if (stack != null) {
                // see note at top of class
                NBTTagCompound stackTag = new NBTTagCompound();
                stack.writeToNBT(stackTag);
                listTag.appendTag(stackTag);
            }
        }
        return listTag;
    }

    public boolean readFromStack() {
        if (!patternStack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound tag = Objects.requireNonNull(patternStack.getTagCompound());
        // may be possible to enter a partially-correct state if setInputs succeeds but setOutputs failed
        // but outside code should treat it as completely incorrect and not attempt to make calls
        return setInputs(readStackArray(tag.getTagList("Inputs", Constants.NBT.TAG_COMPOUND), 9))
                && setOutputs(readStackArray(tag.getTagList("Outputs", Constants.NBT.TAG_COMPOUND), 3));
    }

    private static IAEItemStack[] readStackArray(NBTTagList listTag, int maxCount) {
        // see note at top of class
        IAEItemStack[] stacks = new IAEItemStack[Math.min(listTag.tagCount(), maxCount)];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = AEItemStack.fromNBT(listTag.getCompoundTagAt(i));
        }
        return stacks;
    }

}
