package com.example.juneapp;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.internal.tls.OkHostnameVerifier;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    //Android переменные
    EditText editText;
    TextView temperature,
            cityName,
            description,
            dateTextView;
    Button btn;
    ImageView image;
    LinearLayout linearLayout;
    ProgressBar progressBar;

    //Java переменные
    ArrayList<City> cities;
    boolean isTheSameName=false;
    String temp,
            date,
            condition,
            icon;
    JSONObject jsonConditions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Установка заголовка
        setTitle("Погода");
        //Связь с разметкой
        linearLayout=findViewById(R.id.MainLayout);
        editText=findViewById(R.id.editText);
        cityName=findViewById(R.id.city);
        description=findViewById(R.id.description);
        dateTextView=findViewById(R.id.date);
        image=findViewById(R.id.image);
        temperature=findViewById(R.id.temperature);
        btn=findViewById(R.id.button);
        progressBar=findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        //Расшифровка апи
        fillHashMap();
        //Чтение JSON
        cities=new ArrayList<>();
        try {
            InputStream iS=getResources().openRawResource(R.raw.city);
            String jsonText=readJSON(iS);
            JSONObject jsonObject=new JSONObject(jsonText);
            JSONArray cityArray=jsonObject.getJSONArray("data");
            for (int i=0;i<cityArray.length();i++){
                    JSONObject city=cityArray.getJSONObject(i);
                    if (city.getString("Город").equals("")){
                        cities.add(new City(city.getString("Регион"),city.getString("Широта"),city.getString("Долгота")));
                        Log.d("cityName",city.getString("Регион"));
                    }
                    else {
                        cities.add(new City(city.getString("Город"),city.getString("Широта"),city.getString("Долгота")));
                        Log.d("cityName",city.getString("Город"));
                    }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        //Начальный город

        //Действие кнопки
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MyTask task=new MyTask(MainActivity.this);
                task.execute();
            }
        });
    }
    //Функция чтения файла
    public String readJSON(InputStream iS) throws IOException{

        BufferedReader bR=new BufferedReader(new InputStreamReader(iS));
        StringBuilder sB=new StringBuilder();
        String s=null;
        while ((s=bR.readLine())!=null){
            sB.append(s);
            sB.append("\n");
        }
        Log.d("jsonStr",sB.toString());
        return sB.toString();
    }

    public void fillHashMap(){
        try {
            String conditionsText=readJSON(getResources().openRawResource(R.raw.conditions));
            jsonConditions=new JSONObject(conditionsText);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    class MyTask extends AsyncTask<Void,Void,Void>{

        private WeakReference<MainActivity> mainActivityWeakReference;

        public MyTask(MainActivity mainActivity) {
            this.mainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        protected void onPreExecute() {
            MainActivity activity=mainActivityWeakReference.get();
            activity.btn.setClickable(false);
            activity.progressBar.setVisibility(View.VISIBLE);
            clearTV();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String cityEnter=editText.getText().toString();
            for (City city:cities){
                Log.d("cityK",city.name);
                if (cityEnter.equals(city.name)){
                    isTheSameName=true;
                    //Запрос API
                    OkHttpClient client=new OkHttpClient();
                    String url="https://api.weather.yandex.ru/v1/forecast?lat="+city.latitude
                            +"&lon="+city.longitude+"&lang=ru_RU&extra=true";

                    Request request=new Request.Builder().url(url).
                            header("X-Yandex-API-Key",
                                    "e8b1728e-86bb-45ae-b004-ebfe461f4e4c").build();
                    Call call =client.newCall(request);
                    try {
                        Response response=call.execute();
                        String jsonStr=response.body().string().toString();
                        JSONObject jsonObject=new JSONObject(jsonStr);
                        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("d.MM.yyyy");
                        date=simpleDateFormat.format(jsonObject.getLong("now")*1000);
                        JSONObject fact=jsonObject.getJSONObject("fact");
                        temp=fact.getString("temp");
                        if (Integer.valueOf(temp)>0){
                            temp="+"+temp;
                        }
                        icon="https://yastatic.net/weather/i/icons/blueye/color/svg/"+
                                fact.getString("icon")+".svg.";
                        condition=jsonConditions.getString(fact.getString("condition"));
                        Log.d("weather",temp+"\n"+icon+"\n"+condition);

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!isTheSameName){
                Snackbar.make(linearLayout,"Искомый город не найден",
                        Snackbar.LENGTH_INDEFINITE).
                        setAction("Ок", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        }).show();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MainActivity activity=mainActivityWeakReference.get();
            activity.btn.setClickable(true);
            activity.progressBar.setVisibility(View.INVISIBLE);
            if(isTheSameName){
                activity.temperature.setText(temp);
                activity.dateTextView.setText(date);
                activity.cityName.setText(editText.getText().toString());
                activity.description.setText(condition);
                isTheSameName=false;
            }

        }
        public void clearTV(){
            temperature.setText("");
            dateTextView.setText("");
            cityName.setText("");
            description.setText("");
        }

    }
}