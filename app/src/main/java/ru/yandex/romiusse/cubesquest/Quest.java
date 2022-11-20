package ru.yandex.romiusse.cubesquest;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

public class Quest {

    static TextView text1;
    static MaterialCardView mcv, mcv2;
    static Button button11, button12, button13, button14;

    static TextView text2, hintText2;
    static EditText editText2;

    static TextView text3;
    static ImageView imageView3;

    public Quest(){

    }



    static private void makeInvisible(){
        text1.setVisibility(View.GONE);
        button11.setVisibility(View.GONE); button12.setVisibility(View.GONE); button13.setVisibility(View.GONE); button14.setVisibility(View.GONE);

        text2.setVisibility(View.GONE);
        editText2.setVisibility(View.GONE);

        text3.setVisibility(View.GONE);
        imageView3.setVisibility(View.GONE);


    }

    static private void displayNext(int next){

    }

    private class AnswerInput{

        String text;
        String[] buttons;
        int[] returns;

        public AnswerInput(String text, String[] buttons, int[] returns){
            this.text =text;
            this.buttons = buttons;
            this.returns = returns;
        }

        public void display(){
            makeInvisible();

            text1.setText(text);

            if(buttons.length > 0){
                button11.setText(buttons[0]);
                button11.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        displayNext(returns[0]);
                    }
                });
            }

            if(buttons.length > 1){
                button12.setText(buttons[1]);
                button12.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        displayNext(returns[1]);
                    }
                });
            }

            if(buttons.length > 2){
                button13.setText(buttons[2]);
                button13.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        displayNext(returns[2]);
                    }
                });
            }

            if(buttons.length > 3){
                button14.setText(buttons[3]);
                button14.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        displayNext(returns[3]);
                    }
                });
            }

        }
    }

    private class TextInput{

        String text;
        String answer;
        String[] hints;
        int returns;

        public TextInput(String text, String answer, int returns){

        }

    }

}
