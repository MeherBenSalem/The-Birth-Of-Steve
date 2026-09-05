package com.nightbeam.tbos.client;

import com.google.gson.JsonArray;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Timed, layered client choreography. Danger gets a separate budget from friendly spectacle. */
public final class MemoryEffects {
    public static final int TRAIL=0,NODE=1,IMPACT=2,DANGER=3,RECALL=4,ARC=5,
            WELL=6,RELEASE=7,SHATTER=8,GUARD=9,RECONSTRUCT=10,EMBER=11,SECRET=12,CLEAR=13,STATION=14,DELAY=15;
    private static final List<Effect> LIVE=new ArrayList<>();
    private static long tick,lastSound=-100;
    private static int budget;
    private static boolean reduced;
    private static final class Effect {
        final Vec3 a,b;final int kind;final long seed;int age;
        Effect(Vec3 a,Vec3 b,int kind){this.a=a;this.b=b;this.kind=kind;this.seed=Double.doubleToLongBits(a.x+b.z);}
        int duration(){return switch(kind){case RECALL,RELEASE,SECRET->16;case WELL->5;case STATION->20;case DELAY->12;case SHATTER,RECONSTRUCT,CLEAR->10;case GUARD->8;case ARC->3;default->1;};}
    }
    private MemoryEffects(){}
    public static void clear(){LIVE.clear();tick=0;lastSound=-100;}
    public static void accept(JsonArray events) {
        int count=0;
        for(var value:events) {
            if(++count>32)break;
            JsonArray a=value.getAsJsonArray();if(a.size()!=7)continue;
            Vec3 from=new Vec3(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble());
            Vec3 to=new Vec3(a.get(3).getAsDouble(),a.get(4).getAsDouble(),a.get(5).getAsDouble());
            int kind=a.get(6).getAsInt();
            if(!finite(from)||!finite(to)||kind<0||kind>DELAY)continue;
            if(LIVE.size()>=96) {
                if(kind!=DANGER)continue;
                int discard=-1;for(int i=0;i<LIVE.size();i++)if(LIVE.get(i).kind!=DANGER){discard=i;break;}
                if(discard<0)continue;LIVE.remove(discard);
            }
            LIVE.add(new Effect(from,to,kind));
        }
    }
    private static boolean finite(Vec3 v){return Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z);}
    public static void tick(Minecraft mc) {
        if(mc.level==null||mc.player==null){clear();return;}
        tick++;reduced=YesterglassClientConfig.REDUCED_MOTION.getAsBoolean();
        int quality=YesterglassClientConfig.EFFECT_QUALITY.getAsInt();
        // Mandatory warning geometry is rendered before any friendly decoration.
        budget=192;
        for(Effect e:LIVE)if(e.kind==DANGER)draw(mc,e);
        budget=switch(quality){case 0->16;case 1->64;case 2->160;default->256;};
        for(Effect e:LIVE)if(e.kind!=DANGER)draw(mc,e);
        LIVE.removeIf(e->++e.age>=e.duration());
    }
    private static void draw(Minecraft mc,Effect e) {
        if(mc.player.position().distanceToSqr(e.a)>64*64)return;
        double t=e.age/(double)e.duration();
        Vec3 axis=e.b.subtract(e.a).normalize();if(axis.lengthSqr()<0.01)axis=new Vec3(0,1,0);
        switch(e.kind) {
            case TRAIL,EMBER -> {
                line(mc,e.a,e.b,e.kind==EMBER?ParticleTypes.FLAME:ParticleTypes.END_ROD,5,0);
                if(!reduced) {
                    Vec3 side=axis.cross(new Vec3(0,1,0)).normalize().scale(0.07);
                    line(mc,e.a.add(side),e.b.add(side),ParticleTypes.ELECTRIC_SPARK,3,0);
                }
            }
            case ARC -> {
                Random random=new Random(e.seed);
                Vec3 previous=e.a;
                for(int n=1;n<=7;n++) {
                    Vec3 next=e.a.lerp(e.b,n/7.0);
                    if(n<7)next=next.add((random.nextDouble()-.5)*.4,(random.nextDouble()-.5)*.4,(random.nextDouble()-.5)*.4);
                    line(mc,previous,next,ParticleTypes.ELECTRIC_SPARK,3,0);
                    if(n==3&&!reduced)line(mc,next,next.add(.25,.35,-.2),ParticleTypes.END_ROD,3,0);
                    previous=next;
                }
            }
            case STATION -> {
                if(e.age%4==0){ring(mc,e.a,.8,new Vec3(0,1,0),ParticleTypes.PORTAL,8,reduced?0:t);ring(mc,e.a.add(0,.7,0),.35,new Vec3(0,1,0),ParticleTypes.ENCHANT,6,0);}
            }
            case DELAY -> ring(mc,e.b,reduced?.3:.55*(1-t),new Vec3(0,1,0),ParticleTypes.ENCHANT,6,0);
            case NODE -> ring(mc,e.a,.28,new Vec3(0,1,0),ParticleTypes.ENCHANT,8,0);
            case DANGER -> line(mc,e.a,e.b,ParticleTypes.FLAME,8,0);
            case IMPACT -> {
                ring(mc,e.b,.22,axis,ParticleTypes.CRIT,8,0);
                particle(mc,ParticleTypes.ELECTRIC_SPARK,e.b,new Vec3(0,.025,0));
            }
            case GUARD -> {
                ring(mc,e.a.add(axis.scale(.6)),.65,axis,ParticleTypes.END_ROD,12,t*.1);
                sound(mc,e,1);
            }
            case WELL -> {
                double phase=reduced?0:tick*.24;
                ring(mc,e.a,.45,new Vec3(0,1,0),ParticleTypes.ENCHANT,10,phase);
                if(!reduced)ring(mc,e.a,.62,new Vec3(1,.35,0).normalize(),ParticleTypes.ELECTRIC_SPARK,8,-phase);
            }
            case RECALL,RELEASE -> {
                // Coherent opposing rings collapse or open around the point of release.
                double radius=reduced?.8:e.kind==RECALL?1.25*(1-t)+.1:.3+2.2*t;
                ring(mc,e.a,radius,axis,ParticleTypes.END_ROD,16,t*1.6);
                if(!reduced)ring(mc,e.a,radius*.7,new Vec3(0,1,0),ParticleTypes.ELECTRIC_SPARK,12,-t*2);
                if(e.age%4==0)for(int n=0;n<4;n++) {
                    double a=n*Math.PI/2+t;
                    Vec3 spoke=new Vec3(Math.cos(a)*radius,.2,Math.sin(a)*radius);
                    line(mc,e.a.add(spoke),e.a.add(spoke.scale(.6)),ParticleTypes.ENCHANT,3,0);
                }
                sound(mc,e,e.kind==RELEASE?0:2);
            }
            case SHATTER -> {
                ring(mc,e.a,reduced?.5:.3+t*1.5,new Vec3(0,1,0),ParticleTypes.ELECTRIC_SPARK,12,0);
                if(e.age==0)for(int n=0;n<8;n++)particle(mc,ParticleTypes.CRIT,e.a,new Vec3(Math.cos(n)*.08,.08,Math.sin(n)*.08));
                sound(mc,e,1);
            }
            case RECONSTRUCT -> {
                double height=reduced?.5:t*1.2;
                ring(mc,e.a.add(0,height,0),.65,new Vec3(0,1,0),ParticleTypes.ENCHANT,12,Math.PI/4);
                if(e.age==0)sound(mc,e,2);
            }
            case SECRET,CLEAR -> {
                ring(mc,e.a,reduced?.8:.4+t*2,new Vec3(0,1,0),ParticleTypes.ENCHANT,16,0);
                if(e.age%3==0)particle(mc,ParticleTypes.END_ROD,e.a.add(0,t,0),new Vec3(0,.02,0));
                sound(mc,e,2);
            }
        }
    }
    private static void line(Minecraft mc,Vec3 from,Vec3 to,SimpleParticleType type,int steps,double phase) {
        for(int n=0;n<steps&&budget>0;n++)particle(mc,type,from.lerp(to,n/(double)Math.max(1,steps-1)),Vec3.ZERO);
    }
    private static void ring(Minecraft mc,Vec3 center,double radius,Vec3 normal,SimpleParticleType type,int count,double phase) {
        Vec3 u=normal.cross(Math.abs(normal.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
        Vec3 v=normal.cross(u).normalize();
        for(int n=0;n<count&&budget>0;n++) {
            double a=n*Math.PI*2/count+phase;
            particle(mc,type,center.add(u.scale(Math.cos(a)*radius)).add(v.scale(Math.sin(a)*radius)),Vec3.ZERO);
        }
    }
    private static void particle(Minecraft mc,SimpleParticleType type,Vec3 at,Vec3 motion) {
        // Native particle sprites near the eye become a screen-filling flash.
        if(at.distanceToSqr(mc.player.getEyePosition())<1.0)return;
        if(budget--<=0)return;
        mc.level.addParticle(type,at.x,at.y,at.z,motion.x,motion.y,motion.z);
    }
    private static void sound(Minecraft mc,Effect e,int kind) {
        if(e.age!=0||tick-lastSound<5||mc.player.position().distanceToSqr(e.a)>24*24)return;
        lastSound=tick;
        mc.player.playSound(kind==1?SoundEvents.AMETHYST_BLOCK_HIT:SoundEvents.AMETHYST_BLOCK_CHIME,
            kind==0?.32F:.28F,kind==0?1.55F:1.0F+(e.seed&3)*.06F);
    }
}
