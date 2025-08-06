package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2024-05-26 11:04
 */
@Mixin(Zombie.class)
public abstract class mZombie extends Mob {


    protected mZombie(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Overwrite
    public void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(randomSource, difficulty);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));

    }
    //更多小僵尸
    @Overwrite
    public static boolean getSpawnAsBabyOdds(RandomSource random) {
        return random.nextFloat() < 0.55F;
    }
    //鸡骑
    /*@ModifyConstant(method = "finalizeSpawn",constant = @Constant(doubleValue = 0.05))
    private double moreChickenZombie(double constant){
        return 0.55;
    }*/

}