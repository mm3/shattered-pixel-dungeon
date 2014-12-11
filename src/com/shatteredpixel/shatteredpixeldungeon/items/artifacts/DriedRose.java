package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WraithSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by debenhame on 21/11/2014.
 */
public class DriedRose extends Artifact {

    {
        name = "Dried Rose";
        image = ItemSpriteSheet.ARTIFACT_ROSE1;

        level = 0;
        levelCap = 10;

        charge = 100;
        chargeCap = 100;

        defaultAction = AC_SUMMON;
    }

    protected boolean talkedTo = false;
    protected boolean firstSummon = false;

    public int droppedPetals = 0;

    public static final String AC_SUMMON = "SUMMON";

    @Override
    public ArrayList<String> actions( Hero hero ) {
        ArrayList<String> actions = super.actions( hero );
        if (isEquipped( hero ) && charge == chargeCap)
            actions.add(AC_SUMMON);
        return actions;
    }

    @Override
    public void execute( Hero hero, String action ) {
        if (action.equals(AC_SUMMON)) {

            if (!isEquipped( hero ))      GLog.i("You need to equip your rose to do that.");
            else if (charge != chargeCap) GLog.i("Your rose isn't fully charged yet.");
            else {
                ArrayList<Integer> spawnPoints = new ArrayList<Integer>();
                for (int i = 0; i < Level.NEIGHBOURS8.length; i++) {
                    int p = hero.pos + Level.NEIGHBOURS8[i];
                    if (Actor.findChar(p) == null && (Level.passable[p] || Level.avoid[p])) {
                        spawnPoints.add(p);
                    }
                }

                if (spawnPoints.size() > 0) {
                    GhostHero ghost = new GhostHero();
                    ghost.pos = Random.element(spawnPoints);


                    GameScene.add(ghost, 1f);
                    CellEmitter.get(ghost.pos).start(Speck.factory(Speck.LIGHT), 0.2f, 3);

                    hero.spend(1f);
                    hero.busy();
                    hero.sprite.operate(hero.pos);

                }
            }

        } else{
            super.execute(hero, action);
        }
    }

    @Override
    protected ArtifactBuff passiveBuff() {
        return new roseRecharge();
    }

    public class roseRecharge extends ArtifactBuff {

    }

    public static class Petal extends Item {

        {
            name = "dried petal";
            stackable = true;
            image = ItemSpriteSheet.PETAL;
        }

        @Override
        public boolean doPickUp( Hero hero ) {
            DriedRose rose = hero.belongings.getItem( DriedRose.class );



            if (rose != null && rose.level < rose.levelCap){
                rose.upgrade();
                if (rose.level == rose.levelCap) {
                    GLog.p("The rose is completed!");
                    Sample.INSTANCE.play( Assets.SND_GHOST );
                    GLog.n("sad ghost: Thank you...");
                } else
                    GLog.i("You add the petal to the rose.");
                hero.spendAndNext(TIME_TO_PICK_UP);
                return true;
            } else {
                GLog.w("You have no rose to add this petal to.");
                return false;
            }
        }

        @Override
        public String info() {
            return "A frail dried up petal, which has somehow survived this far into the dungeon.";
        }

    }

    //TODO: needs to:
    //have combat stats
    //attack only nearby enemies
    //Be tethered to the player
    //Enemies must be able/want to attack it
    //Must be lost on level transition.
    public static class GhostHero extends NPC {

        {
            name = "sad ghost";
            spriteClass = WraithSprite.class;

            flying = true;

            state = WANDERING;
            enemy = DUMMY;

        }

        private static final String TXT_WELCOME = "My spirit is bound to this rose, it was very precious to me, a gift " +
                "from my love whom I left on the surface.\n\nI cannot return to him, but thanks to you I have a second " +
                "chance to complete my journey. When I am able I will respond to your call and fight with you.\n\n" +
                "hopefully you may succeed where I failed...";

        @Override
        public String defenseVerb() {
            return "evaded";
        }

        @Override
        protected boolean getCloser( int target ) {
            if (state == WANDERING)
                this.target = target = Dungeon.hero.pos;
            return super.getCloser( target );
        }

        @Override
        protected Char chooseEnemy() {
            if (enemy == DUMMY || !enemy.isAlive()) {

                HashSet<Mob> enemies = new HashSet<Mob>();
                for (Mob mob : Dungeon.level.mobs) {
                    if (mob.hostile && Level.fieldOfView[mob.pos]) {
                        enemies.add(mob);
                    }
                }
                enemy = enemies.size() > 0 ? Random.element( enemies ) : DUMMY;
            }
            return enemy;
        }

        @Override
        public void damage( int dmg, Object src ) {
        }

        @Override
        public void add( Buff buff ) {
        }

        @Override
        public void interact() {
            //if (!talkedTo){
            //    talkedTo = true;
            //    GameScene.show(new WndQuest(this, TXT_WELCOME));
            //} else {
                int curPos = pos;

                moveSprite( pos, Dungeon.hero.pos );
                move( Dungeon.hero.pos );

                Dungeon.hero.sprite.move( Dungeon.hero.pos, curPos );
                Dungeon.hero.move( curPos );

                Dungeon.hero.spend( 1 / Dungeon.hero.speed() );
                Dungeon.hero.busy();
            //}
        }
    }

    //************************************************************************************
    //This is a bunch strings & string arrays, used in all of the sad ghost's voice lines.
    //************************************************************************************

    public static final String GHOST_HELLO = "Hello again " + Dungeon.hero.className() + ".";

    //enum, for clarity.
    public static enum DEPTHS{
        SEWERS,
        PRISON,
        CAVES,
        CITY,
        HALLS,
        AMULET
    }

    //1st index - depth type, 2nd index - specific line.
    public static final String[][] GHOST_VOICE_AMBIENT = {
            {
                    "These sewers were once safe, some even lived here in the winter...",
                    "I wonder what happened to the guard patrols, did they give up?...",
                    "I had family on the surface, I hope they are safe..."
            },{
                    "I've heard stories about this place, nothing good...",
                    "This place was always more of a dungeon than a prison...",
                    "I can't imagine what went on when this place was abandoned..."
            },{
                    "No human or dwarf has been here for a very long time...",
                    "Something must have gone very wrong, for the dwarves to abandon a gold mine...",
                    "I feel great evil lurking below..."
            },{
                    "The dwarves were industrious, but greedy...",
                    "I hope the surface never ends up like this place...",
                    "So the dwarvern metropolis really has fallen..."
            },{
                    "What is this place?...",
                    "So the stories are true, we have to fight a demon god...",
                    "I feel a great evil in this place..."
            },{
                    "... I don't like this place... We should leave as soon as possible..."
            }
    };

    //1st index - depth type, 2nd index - boss or not, 3rd index - specific line.
    public static final String[][][] GHOST_VOICE_ENEMIES = {
            {
                {
                    "Let's make the sewers safe again...",
                    "If the guards couldn't defeat them, perhaps we can...",
                    "These crabs are extremely annoying..."
                },{
                    "Beware Goo!...",
                    "Many of my friends died to this thing, time for vengeance...",
                    "Such an abomination cannot be allowed to live..."
                }
            },{
                {
                    "What dark magic happened here?...",
                    "To think the captives of this place are now its guardians...",
                    "They were criminals before, now they are monsters..."
                },{
                    "If only he would see reason, he doesn't seem insane...",
                    "He assumes we are hostile, if only he would stop to talk...",
                    "The one prisoner left sane is a deadly assassin. Of course..."
                }
            },{
                {
                    "The creatures here are twisted, just like the sewers... ",
                    "more gnolls, I hate gnolls...",
                    "Even the bats are bloodthirsty here..."
                },{
                    "Only dwarves would build a mining machine that kills looters...",
                    "That thing is huge...",
                    "How has it survived here for so long?..."
                }
            },{
                {
                    "Dwarves aren't supposed to look that pale...",
                    "I don't know what's worse, the dwarves, or their creations...",
                    "They all obey their master without question, even now..."
                },{
                    "When people say power corrupts, this is what they mean...",
                    "He's more a Lich than a King now...",
                    "Looks like he's more demon than dwarf now..."
                }
            },{
                {
                    "What the heck is that thing?...",
                    "This place is terrifying...",
                    "What were the dwarves thinking, toying with power like this?..."
                },{
                    "Oh.... this doesn't look good...",
                    "So that's what a god looks like?...",
                    "This is going to hurt..."
                }
            },{
                {
                    "Hello source viewer, I'm writing this here as this line should never trigger. Have a nice day!"
                },{
                    "Hello source viewer, I'm writing this here as this line should never trigger. Have a nice day!"
                }
            }
    };

    //1st index - Yog or not, 2nd index - specific line.
    public static final String[][] GHOST_VOICE_BOSSBEATEN = {
            {
                    "Yes!",
                    "Victory!"
            },{
                    "It's over... we won...",
                    "I can't believe it... We just killed a god..."
            }
    };

    //1st index - boss or not, 2nd index - specific line.
    public static final String[][] GHOST_VOICE_DEFEATED = {
            {
                    "Good luck...",
                    "I will return...",
                    "Tired... for now..."
            },{
                    "No... I can't....",
                    "I'm sorry.. good luck..",
                    "Finish it off... without me..."
            }
    };

    public static final String[] GHOST_VOICE_HEROKILLED = {
                    Dungeon.hero.curAction + ", nooo...",
                    "no...",
                    "I couldn't help them..."
    };

    public static final String[] GHOST_VOICE_BLESSEDANKH = {
                    "Incredible!...",
                    "Wish I had one of those...",
                    "How did you survive that?..."
    };
}