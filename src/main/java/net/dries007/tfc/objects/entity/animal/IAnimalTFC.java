/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.objects.entity.animal;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;

import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import net.dries007.tfc.ConfigTFC;
import net.dries007.tfc.util.OreDictionaryHelper;
import net.dries007.tfc.util.calendar.CalendarTFC;

/**
 * Interface for animals with gender, familiarity and aging
 */
public interface IAnimalTFC extends ICreatureTFC
{
    /**
     * Get this animal gender, female or male
     *
     * @return Gender of this animal
     */
    Gender getGender();

    /**
     * Set this animal gender, used on spawn/birth
     *
     * @param gender the Gender to set to
     */
    void setGender(Gender gender);

    /**
     * Returns the birth day of this animal. Determines how old this animal is
     *
     * @return returns the day this animal has been birth
     */
    int getBirthDay();

    /**
     * Sets the birth day of this animal. Used to determine how old this animal is
     *
     * @param value the day this animal has been birth. Used when this animal spawns.
     */
    void setBirthDay(int value);

    /**
     * What is the maximum familiarity obtainable for adults of this animal?
     *
     * @return 0 if not familiarizable at all, ]0, 1] for a cap
     */
    default float getAdultFamiliarityCap()
    {
        return 0;
    }

    /**
     * Returns the familiarity of this animal
     *
     * @return float value between 0-1.
     */
    float getFamiliarity();

    /**
     * Set this animal familiarity
     *
     * @param value float value between 0-1.
     */
    void setFamiliarity(float value);

    /**
     * Returns true if this female is pregnant, or the next time it ovulates, eggs are fertilized.
     *
     * @return true if this female has been fertilized.
     */
    boolean isFertilized();

    /**
     * Set if this female is fertilized
     *
     * @param value true on fertilization (mating)
     */
    void setFertilized(boolean value);

    /**
     * Event: Do things on fertilization of females (ie: save the male genes for some sort of genetic selection)
     */
    default void onFertilized(@Nonnull IAnimalTFC male)
    {
        setFertilized(true);
    }

    /**
     * Used by model renderer to scale the size of the animal
     *
     * @return double value between 0(birthday) to 1(full grown adult)
     */
    default double getPercentToAdulthood()
    {
        double value = (CalendarTFC.PLAYER_TIME.getTotalDays() - this.getBirthDay()) / (double) getDaysToAdulthood();
        if (value > 1) value = 1;
        if (value < 0) value = 0;
        return value;
    }

    /**
     * Get this entity age, based on birth
     *
     * @return the Age enum of this entity
     */
    default Age getAge()
    {
        long deltaDays = CalendarTFC.PLAYER_TIME.getTotalDays() - this.getBirthDay();
        if (getAdultFamiliarityCap() > 0 && ConfigTFC.GENERAL.enableAnimalAging && deltaDays > getDaysToAdulthood() * ConfigTFC.GENERAL.factorAnimalAging)
        {
            return Age.OLD; // if enabled, only for familiarizable animals
        }
        else if (deltaDays > getDaysToAdulthood())
        {
            return Age.ADULT;
        }
        else
        {
            return Age.CHILD;
        }
    }

    /**
     * Get the number of days needed for this animal to be adult
     *
     * @return number of days
     */
    int getDaysToAdulthood();

    /**
     * Check if this animal is ready to mate
     *
     * @return true if ready
     */
    default boolean isReadyToMate()
    {
        return this.getAge() == Age.ADULT && !(this.getFamiliarity() < 0.3f) && !this.isFertilized() && !this.isHungry();
    }

    /**
     * Check if said item can feed this animal
     *
     * @param stack the itemstack to check
     * @return true if item is used to feed this animal (entice and increase familiarity)
     */
    default boolean isFood(@Nonnull ItemStack stack)
    {
        return OreDictionaryHelper.doesStackMatchOre(stack, "grain");
    }

    /**
     * Is this animal hungry?
     *
     * @return true if this animal can be fed by player
     */
    boolean isHungry();

    /**
     * Which animal type is this? Do this animal lay eggs or give birth to it's offspring?
     *
     * @return the enum Type of this animal.
     */
    Type getType();

    /**
     * Some animals can give products (eg: Milk, Wool and Eggs)
     * This function returns if said animal is ready to be worked upon
     * (or if it is ready to lay eggs on it's own)
     *
     * ** Check for everything **
     * this function should return only true if the animal will give it's products upon work
     * (so TOP integration could show this animal is ready)
     *
     * @return true if it is ready for product production
     */
    default boolean isReadyForAnimalProduct()
    {
        return false;
    }

    /**
     * Get the products of this animal
     * Can return more than one item itemstack
     * fortune and other behaviour should not be handled here
     * Suggestion: EntityLiving#processInteract() for right clicking handling
     *
     * (This function should be implemented with TOP integration in mind ie: what would
     *  you like for the tooltip to show when #isReadyForAnimalProduct returns true?)
     *
     * @return a list of itemstack
     */
    default List<ItemStack> getProducts()
    {
        return Collections.emptyList();
    }

    /**
     * Get the tooltip for ** Why this animal is not ready? **
     * Common usages: Cows not having milk, chickens already layed eggs today, sheeps' fleece not grown, not enough familiarity
     *
     * @return null if you don't want for a tooltip to be shown, any TextComponentTranslation object if you want it to.
     */
    default TextComponentTranslation getTooltip() { return null; }

    /**
     * Get the animal name, which can be determined by male / female
     * (eg: bull or cow, rooster or chicken)
     *
     * @return the TextComponentTranslation for localized name
     */
    TextComponentTranslation getAnimalName();

    enum Age
    {
        CHILD, ADULT, OLD
    }

    enum Gender
    {
        MALE, FEMALE;

        public static Gender fromBool(boolean value)
        {
            return value ? MALE : FEMALE;
        }

        public boolean toBool()
        {
            return this == MALE;
        }
    }

    enum Type
    {
        MAMMAL, OVIPAROUS
    }

    /**
     * Helper enum with some default grouping rules for animals
     */
    enum AnimalGroupingRules
    {
        MOTHER_AND_CHILDREN_OR_SOLO_MALE // One individual group = male / Two or more = Mother and children
            {
                @Override
                public BiConsumer<List<EntityLiving>, Random> getRule()
                {
                    return (entityLivings, random) ->
                    {
                        for (int i = 0; i < entityLivings.size(); i++)
                        {
                            EntityLiving living = entityLivings.get(i);
                            if (living instanceof IAnimalTFC)
                            {
                                IAnimalTFC animal = (IAnimalTFC) living;
                                if (i == 0)
                                {
                                    // Mother
                                    int lifeTimeDays = animal.getDaysToAdulthood() + random.nextInt((int) Math.max(1, animal.getDaysToAdulthood() * (ConfigTFC.GENERAL.factorAnimalAging - 1)));
                                    animal.setGender(entityLivings.size() > 1 ? Gender.FEMALE : Gender.MALE);
                                    animal.setBirthDay((int) (CalendarTFC.PLAYER_TIME.getTotalDays() - lifeTimeDays));
                                }
                                else
                                {
                                    // Children
                                    int lifeTimeDays = random.nextInt(animal.getDaysToAdulthood());
                                    animal.setGender(Gender.fromBool(random.nextBoolean()));
                                    animal.setBirthDay((int) (CalendarTFC.PLAYER_TIME.getTotalDays() - lifeTimeDays));
                                }
                            }
                        }
                    };
                }
            },
        ELDER_AND_POPULATION // First always adult
            {
                @Override
                public BiConsumer<List<EntityLiving>, Random> getRule()
                {
                    return (entityLivings, random) ->
                    {
                        for (int i = 0; i < entityLivings.size(); i++)
                        {
                            EntityLiving living = entityLivings.get(i);
                            if (living instanceof IAnimalTFC)
                            {
                                IAnimalTFC animal = (IAnimalTFC) living;
                                if (i == 0)
                                {
                                    // Elder
                                    int lifeTimeDays = animal.getDaysToAdulthood() + random.nextInt((int) Math.max(1, animal.getDaysToAdulthood() * (ConfigTFC.GENERAL.factorAnimalAging)));
                                    animal.setGender(Gender.fromBool(random.nextBoolean()));
                                    animal.setBirthDay((int) (CalendarTFC.PLAYER_TIME.getTotalDays() - lifeTimeDays));
                                }
                                else
                                {
                                    int lifeTimeDays = random.nextInt((int) Math.max(1, animal.getDaysToAdulthood() * (ConfigTFC.GENERAL.factorAnimalAging - 1)));
                                    animal.setGender(Gender.fromBool(random.nextBoolean()));
                                    animal.setBirthDay((int) (CalendarTFC.PLAYER_TIME.getTotalDays() - lifeTimeDays));
                                }
                            }
                        }
                    };
                }
            },
        MALE_AND_FEMALES // One adult male (or solo males) + random females
            {
                @Override
                public BiConsumer<List<EntityLiving>, Random> getRule()
                {
                    return (entityLivings, random) ->
                    {
                        for (int i = 0; i < entityLivings.size(); i++)
                        {
                            EntityLiving living = entityLivings.get(i);
                            if (living instanceof IAnimalTFC)
                            {
                                IAnimalTFC animal = (IAnimalTFC) living;
                                if (i == 0)
                                {
                                    // Male
                                    int lifeTimeDays = animal.getDaysToAdulthood() + random.nextInt((int) Math.max(1, animal.getDaysToAdulthood() * (ConfigTFC.GENERAL.factorAnimalAging - 1)));
                                    animal.setGender(Gender.MALE);
                                    animal.setBirthDay((int) (CalendarTFC.PLAYER_TIME.getTotalDays() - lifeTimeDays));
                                }
                                else
                                {
                                    int lifeTimeDays = random.nextInt((int) Math.max(1, animal.getDaysToAdulthood() * (ConfigTFC.GENERAL.factorAnimalAging - 1)));
                                    animal.setGender(Gender.FEMALE);
                                    animal.setBirthDay((int) (CalendarTFC.PLAYER_TIME.getTotalDays() - lifeTimeDays));
                                }
                            }
                        }
                    };
                }
            };

        abstract BiConsumer<List<EntityLiving>, Random> getRule();
    }
}
