package com.millebornes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameConstants {

    public static final String SETTINGS_FILE = "1000_mile_card_settings.properties"; // Changed to .properties for Java Properties class

    public static final Map<HazardEffect, HazardEffect> HAZARD_REMEDY_MAP;
    public static final Map<HazardEffect, HazardEffect> REMEDY_HAZARD_MAP; // Added for reverse lookup
    public static final Map<HazardEffect, HazardEffect> HAZARD_SAFETY_MAP;

    static {
        Map<HazardEffect, HazardEffect> hrm = new HashMap<>();
        hrm.put(HazardEffect.STOP, HazardEffect.GO);
        hrm.put(HazardEffect.SPEED_LIMIT, HazardEffect.END_OF_SPEED_LIMIT);
        hrm.put(HazardEffect.FLAT_TIRE, HazardEffect.SPARE_TIRE);
        hrm.put(HazardEffect.ACCIDENT, HazardEffect.REPAIRS);
        hrm.put(HazardEffect.OUT_OF_GAS, HazardEffect.GASOLINE);
        HAZARD_REMEDY_MAP = Collections.unmodifiableMap(hrm);

        Map<HazardEffect, HazardEffect> rh = new HashMap<>();
        for (Map.Entry<HazardEffect, HazardEffect> entry : hrm.entrySet()) {
            rh.put(entry.getValue(), entry.getKey());
        }
        REMEDY_HAZARD_MAP = Collections.unmodifiableMap(rh);


        Map<HazardEffect, HazardEffect> hsm = new HashMap<>();
        hsm.put(HazardEffect.STOP, HazardEffect.RIGHT_OF_WAY);
        hsm.put(HazardEffect.SPEED_LIMIT, HazardEffect.RIGHT_OF_WAY); // Right of Way covers both Stop and Speed Limit
        hsm.put(HazardEffect.FLAT_TIRE, HazardEffect.PUNCTURE_PROOF);
        hsm.put(HazardEffect.ACCIDENT, HazardEffect.DRIVING_ACE);
        hsm.put(HazardEffect.OUT_OF_GAS, HazardEffect.EXTRA_TANK);
        HAZARD_SAFETY_MAP = Collections.unmodifiableMap(hsm);
    }

    public static List<Card> createDeckCards() {
        List<Card> cards = new ArrayList<>();

        // Distance Cards (Total 46)
        cards.addAll(Collections.nCopies(10, new Card("25 Miles", CardType.DISTANCE, 25, HazardEffect.NONE)));
        cards.addAll(Collections.nCopies(10, new Card("50 Miles", CardType.DISTANCE, 50, HazardEffect.NONE)));
        cards.addAll(Collections.nCopies(10, new Card("75 Miles", CardType.DISTANCE, 75, HazardEffect.NONE)));
        cards.addAll(Collections.nCopies(12, new Card("100 Miles", CardType.DISTANCE, 100, HazardEffect.NONE)));
        cards.addAll(Collections.nCopies(4, new Card("200 Miles", CardType.DISTANCE, 200, HazardEffect.NONE)));

        // Hazard Cards (Total 18)
        cards.addAll(Collections.nCopies(5, new Card("Stop", CardType.HAZARD, HazardEffect.STOP)));
        cards.addAll(Collections.nCopies(4, new Card("Speed Limit", CardType.HAZARD, HazardEffect.SPEED_LIMIT)));
        cards.addAll(Collections.nCopies(3, new Card("Flat Tire", CardType.HAZARD, HazardEffect.FLAT_TIRE)));
        cards.addAll(Collections.nCopies(3, new Card("Accident", CardType.HAZARD, HazardEffect.ACCIDENT)));
        cards.addAll(Collections.nCopies(3, new Card("Out of Gas", CardType.HAZARD, HazardEffect.OUT_OF_GAS)));

        // Remedy Cards (Total 30)
        cards.addAll(Collections.nCopies(6, new Card("Go", CardType.REMEDY, HazardEffect.GO)));
        cards.addAll(Collections.nCopies(6, new Card("End of Speed Limit", CardType.REMEDY, HazardEffect.END_OF_SPEED_LIMIT)));
        cards.addAll(Collections.nCopies(6, new Card("Spare Tire", CardType.REMEDY, HazardEffect.SPARE_TIRE)));
        cards.addAll(Collections.nCopies(6, new Card("Repairs", CardType.REMEDY, HazardEffect.REPAIRS)));
        cards.addAll(Collections.nCopies(6, new Card("Gasoline", CardType.REMEDY, HazardEffect.GASOLINE)));

        // Safety Cards (Total 4)
        cards.add(new Card("Right of Way", CardType.SAFETY, HazardEffect.RIGHT_OF_WAY));
        cards.add(new Card("Puncture Proof", CardType.SAFETY, HazardEffect.PUNCTURE_PROOF));
        cards.add(new Card("Driving Ace", CardType.SAFETY, HazardEffect.DRIVING_ACE));
        cards.add(new Card("Extra Tank", CardType.SAFETY, HazardEffect.EXTRA_TANK));

        return cards;
    }
}