package tbos.gradle

import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Rewrites the JSON that shared/ authors in the 26.1.2 shape into the shape
 * Minecraft 26.3 reads, on the way into the jar. Applied by
 * multiloader-common.gradle's processResources through the two filters below,
 * the same way 1.20.1's build relocates its plural data-pack directories:
 * the delta is mechanical, so ~120 data files are not forked into overrides/.
 *
 * The rules are the ones Mojang applied to the vanilla data pack between 26.2
 * and 26.3 (compare the data/ trees of the two NeoForm source jars):
 *
 * <ul>
 * <li>A condition list {@code "conditions": [{"condition": "t", ...}, ...]}
 *     becomes one condition {@code "condition": {"type": "t", ...}}; several
 *     are wrapped in {@code {"type": "minecraft:all_of", "terms": [...]}}. An
 *     empty list is dropped. Nested {@code terms}/{@code term} are rewritten
 *     too.</li>
 * <li>Any other key holding such a list (an advancement criterion's
 *     {@code player}/{@code entity}/... context predicate) keeps its name and
 *     gets the single merged condition as its value.</li>
 * <li>{@code "functions": [{"function": "t", ...}, ...]} becomes
 *     {@code "modifier": {"type": "t", ...}}, or an inline list when there are
 *     several. Inside a {@code minecraft:sequence} function the list keeps its
 *     {@code functions} name.</li>
 * <li>{@code rolls} is an int provider now: whole-number floats become ints.</li>
 * <li>Block-model elements: {@code "shade": false} becomes
 *     {@code "shade_direction_override": "up"} (26.3 ignores {@code shade}
 *     silently, which would darken faces authored as unshaded).</li>
 * </ul>
 */
final class Json263 {
    private static final JsonGenerator JSON = new JsonGenerator.Options()
            .disableUnicodeEscaping()
            .build()

    private Json263() {
    }

    /** Returns {@code json} untouched when no rule applies, so only files that need it are rewritten. */
    static String rewrite(String json, Closure<Object> upgrade) {
        Object tree = new JsonSlurper().parseText(json)
        String before = JSON.toJson(tree)
        String after = JSON.toJson(upgrade(tree))
        return after == before ? json : JsonOutput.prettyPrint(after, true) + '\n'
    }

    // ---- data packs: loot tables, advancements, predicates, item modifiers --

    static Object upgradeData(Object node) {
        if (node instanceof List) {
            return node.collect { upgradeData(it) }
        }
        if (!(node instanceof Map)) {
            return node
        }
        Map out = new LinkedHashMap()
        ((Map) node).each { key, value ->
            if (key == 'conditions' && value instanceof List && value.isEmpty()) {
                return // an empty condition list is the absent condition
            }
            if (isConditionList(value)) {
                out.put(key == 'conditions' ? 'condition' : key, mergeConditions((List) value))
            } else if (key == 'functions' && isFunctionList(value)) {
                List functions = ((List) value).collect { upgradeFunction((Map) it) }
                out.put('modifier', functions.size() == 1 ? functions[0] : functions)
            } else if (key == 'rolls') {
                out.put(key, wholeNumbersToInts(value))
            } else {
                out.put(key, upgradeData(value))
            }
        }
        return out
    }

    private static boolean isConditionList(Object value) {
        value instanceof List && !value.isEmpty() &&
                value.every { it instanceof Map && it.get('condition') instanceof String }
    }

    private static boolean isFunctionList(Object value) {
        value instanceof List && !value.isEmpty() &&
                value.every { it instanceof Map && it.get('function') instanceof String }
    }

    private static Object mergeConditions(List conditions) {
        List upgraded = conditions.collect { upgradeCondition((Map) it) }
        if (upgraded.size() == 1) {
            return upgraded[0]
        }
        Map allOf = new LinkedHashMap()
        allOf.put('type', 'minecraft:all_of')
        allOf.put('terms', upgraded)
        return allOf
    }

    private static Map upgradeCondition(Map condition) {
        Map out = new LinkedHashMap()
        out.put('type', condition.get('condition'))
        condition.each { key, value ->
            if (key == 'condition') {
                return
            }
            if (key == 'terms' && value instanceof List) {
                out.put(key, value.collect { it instanceof Map && it.get('condition') instanceof String ? upgradeCondition((Map) it) : upgradeData(it) })
            } else if (key == 'term' && value instanceof Map && value.get('condition') instanceof String) {
                out.put(key, upgradeCondition((Map) value))
            } else {
                out.put(key, upgradeData(value))
            }
        }
        return out
    }

    private static Map upgradeFunction(Map function) {
        Map out = new LinkedHashMap()
        String type = function.get('function')
        out.put('type', type)
        function.each { key, value ->
            if (key == 'function') {
                return
            }
            if (key == 'functions' && type == 'minecraft:sequence' && isFunctionList(value)) {
                out.put(key, ((List) value).collect { upgradeFunction((Map) it) })
            } else if (key == 'conditions' && value instanceof List && value.isEmpty()) {
                return
            } else if (isConditionList(value)) {
                out.put(key == 'conditions' ? 'condition' : key, mergeConditions((List) value))
            } else {
                out.put(key, upgradeData(value))
            }
        }
        return out
    }

    private static Object wholeNumbersToInts(Object value) {
        if (value instanceof Number) {
            BigDecimal decimal = value as BigDecimal
            return decimal.stripTrailingZeros().scale() <= 0 ? decimal.intValueExact() : value
        }
        if (value instanceof Map) {
            Map out = new LinkedHashMap()
            ((Map) value).each { key, inner ->
                out.put(key, key in ['min', 'max', 'value'] ? wholeNumbersToInts(inner) : upgradeData(inner))
            }
            return out
        }
        return upgradeData(value)
    }

    // ---- block models ------------------------------------------------------

    static Object upgradeModel(Object node) {
        if (node instanceof List) {
            return node.collect { upgradeModel(it) }
        }
        if (!(node instanceof Map)) {
            return node
        }
        Map out = new LinkedHashMap()
        ((Map) node).each { key, value ->
            if (key == 'elements' && value instanceof List) {
                out.put(key, value.collect { upgradeElement(it) })
            } else {
                out.put(key, value)
            }
        }
        return out
    }

    private static Object upgradeElement(Object element) {
        if (!(element instanceof Map) || !(((Map) element).get('shade') instanceof Boolean)) {
            return element
        }
        Map out = new LinkedHashMap()
        ((Map) element).each { key, value ->
            if (key == 'shade') {
                if (value == false && !((Map) element).containsKey('shade_direction_override')) {
                    out.put('shade_direction_override', 'up')
                }
            } else {
                out.put(key, value)
            }
        }
        return out
    }
}

/** processResources filter for data-pack JSON; see {@link Json263}. */
class DataPackJson263Filter extends FilterReader {
    DataPackJson263Filter(Reader input) {
        super(new StringReader(Json263.rewrite(input.text, Json263.&upgradeData)))
    }
}

/** processResources filter for block-model JSON; see {@link Json263}. */
class ModelJson263Filter extends FilterReader {
    ModelJson263Filter(Reader input) {
        super(new StringReader(Json263.rewrite(input.text, Json263.&upgradeModel)))
    }
}
