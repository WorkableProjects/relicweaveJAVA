package com.relicweave.quest;

import com.google.gson.JsonObject;
import com.relicweave.RelicWeave;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

/**
 * Advancement trigger {@code relicweave:quest} for quest-line steps that no vanilla trigger covers (a gate woken, a
 * relic used, an augment woven...). Criteria name the step: {@code {"trigger": "relicweave:quest", "conditions":
 * {"event": "gate_activated"}}}. Fired only from server code through {@link QuestEvents#fire}.
 */
public final class QuestTrigger extends SimpleCriterionTrigger<QuestTrigger.Instance> {
    public static final ResourceLocation ID = RelicWeave.id("quest");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Instance(player, GsonHelper.getAsString(json, "event"));
    }

    public void trigger(ServerPlayer player, String event) {
        trigger(player, instance -> instance.event.equals(event));
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final String event;

        public Instance(ContextAwarePredicate player, String event) {
            super(ID, player);
            this.event = event;
        }

        public String event() {
            return event;
        }

        @Override
        public JsonObject serializeToJson(net.minecraft.advancements.critereon.SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("event", event);
            return json;
        }
    }
}
