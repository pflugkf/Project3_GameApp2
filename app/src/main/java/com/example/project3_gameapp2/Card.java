package com.example.project3_gameapp2;

import java.util.Random;

public class Card {
    String value;//0-9, skip, draw 4
    String color;//red, yellow, green, blue, black for draw 4
    String cardID;

    public Card() {
        //generate card of random value and color
        Random random = new Random();
        int cardValue = random.nextInt(12);
        String[] colorSet = {"Red", "Green", "Yellow", "Blue"};

        if(cardValue <= 9) {
            this.value = String.valueOf(cardValue);
        } else if (cardValue == 10) {
            this.value = "Skip";
        } else {
            this.value = "Draw 4";
        }

        if(this.value.equals("Draw 4")) {
            this.color = "Black";
        } else {
            this.color = colorSet[random.nextInt(4)];
        }

        this.cardID = "";
    }

    public Card(String value, String color, String cardID) {
        this.value = value;
        this.color = color;
        this.cardID = cardID;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getCardID() {
        return cardID;
    }

    public void setCardID(String cardID) {
        this.cardID = cardID;
    }

    public String makeCardString(Card card) {
        return card.getValue() + "," + card.getColor();
    }
}
