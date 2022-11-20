package ru.yandex.romiusse.cubesquest;

import static android.util.Base64.DEFAULT;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    TextView distanceText;

    public static GPSTracker gpsTracker;

    ActivityResultLauncher<Intent> someActivityResultLauncher;


    MyRecyclerViewAdapter adapter;
    ArrayList<ArrayList<String>> editorTasks = new ArrayList<>();
    JSONObject editorTasksObject = new JSONObject();
    private int editorTaskNum = 0;
    private String questName = "";
    private String questStartId = "";
    private int editorSlidesTaskNum = 0, editorSlidesPosition = 0;

    private void initRedactor() {
        ArrayList<String> redactorList = new ArrayList<>();
        ArrayAdapter<String> redactorAdapter;
        ListView listView = findViewById(R.id.redactorList);

        Button editChooseLayout3CreateNew = findViewById(R.id.editChooseLayout3CreateNew);
        editChooseLayout3CreateNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorTasksObject = new JSONObject();
                editorTasks = new ArrayList<>();
                editorTaskNum = 0;
                questName = "";
                questStartId = "";
                setContentView(R.layout.editor_list);
                initEditor();

            }
        });

        Button editChooseLayout3ButtonExit = findViewById(R.id.editChooseLayout3ButtonExit);
        editChooseLayout3ButtonExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.activity_main);
                initHome();
            }
        });

        String path = Environment.getExternalStorageDirectory() + "/CubesQuest";
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (int i = 0; i < Objects.requireNonNull(files).length; i++) {
            if (files[i].getAbsolutePath().substring(files[i].getAbsolutePath().lastIndexOf(".")).equals(".quest")) {
                redactorList.add(files[i].getName());
                //Log.d("Files", files[i].getName() + " Added!");
            }
            //Log.d("Files", "FileName:" + files[i].getName());
        }
        redactorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, redactorList);
        listView.setAdapter(redactorAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                    long id) {
                String json = readFromFile(MainActivity.this, ((TextView) itemClicked).getText().toString());
                try {
                    JSONObject rawobj = new JSONObject(json);
                    editorTasksObject = (JSONObject) rawobj.get("data");
                    setContentView(R.layout.editor_list);

                    JSONArray names = editorTasksObject.names();
                    List<Integer> namesInt = new ArrayList<>();
                    for(int i = 0;i<names.length();i++) namesInt.add(Integer.parseInt((String) names.get(i)));
                    namesInt.sort(Comparator.naturalOrder());



                    for (int i = 0; i < namesInt.size(); i++) {
                        JSONObject obj = (JSONObject) editorTasksObject.get(namesInt.get(i).toString());
                        if (obj.get("type").equals("answerInput")) {
                            JSONArray buttonsArray = (JSONArray) obj.get("buttons");
                            List<Integer> idsto = new ArrayList<>();
                            for (int j = 0; j < buttonsArray.length(); j++) {
                                idsto.add(Integer.parseInt(((JSONObject) buttonsArray.get(j)).get("idto").toString()));
                            }
                            editorTasks.add(new ArrayList<String>(Arrays.asList("Выбор ответов", "answerInput", "ID: " + obj.get("id"), "Переход: " + idsto.toString(), "" + obj.get("id"))));
                        } else if (obj.get("type").equals("textInput")) {
                            editorTasks.add(new ArrayList<String>(Arrays.asList("Текстовый ответ", "textInput", "ID: " + obj.get("id"), "Переход: " + obj.get("idto"), "" + obj.get("id"))));
                        } else if (obj.get("type").equals("slides")) {
                            editorTasks.add(new ArrayList<String>(Arrays.asList("Слайды", "slides", "ID: " + obj.get("id"), "Переход: " + obj.get("idto"), "" + obj.get("id"))));
                        } else {
                            editorTasks.add(new ArrayList<String>(Arrays.asList("Геолокация", "mapFlag", "ID: " + obj.get("id"), "Переход: " + obj.get("idto"), "" + obj.get("id"))));
                        }
                    }
                    editorTaskNum = (int) rawobj.get("editorTaskNum");
                    questName = rawobj.get("name").toString();
                    questStartId = rawobj.get("startid").toString();


                    initEditor();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static String readFromFile(Context contect, String nameFile) {
        String aBuffer = "";
        try {
            File myFile = new File(Environment.getExternalStorageDirectory() + "/CubesQuest/" + nameFile);
            FileInputStream fIn = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
            String aDataRow = "";
            while ((aDataRow = myReader.readLine()) != null) {
                aBuffer += aDataRow;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return aBuffer;
    }

    private void initEditor() {
        // data to populate the RecyclerView with
        RecyclerView recyclerView = findViewById(R.id.EditRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        EditText editText = findViewById(R.id.EditQuestName);
        editText.setText(questName);

        EditText startId = findViewById(R.id.EditQuestStartId);
        startId.setText(questStartId);

        Button saveData = findViewById(R.id.EditSave);
        saveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                questName = editText.getText().toString();
                questStartId = startId.getText().toString();
                if (questName.length() == 0) {
                    Toast.makeText(MainActivity.this, "Введите название вашего квеста", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (editorTasks.size() == 0) {
                    Toast.makeText(MainActivity.this, "Должна быть минимум одна страничка квеста", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (startId.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "Должен быть указан стартовый id", Toast.LENGTH_SHORT).show();
                    return;
                }

                File externalAppDir = new File(Environment.getExternalStorageDirectory() + "/CubesQuest");
                if (!externalAppDir.exists()) {
                    externalAppDir.mkdir();
                }

                File file = new File(externalAppDir, questName + ".quest");
                try {
                    file.createNewFile();
                    JSONObject quest = new JSONObject();
                    quest.put("data", editorTasksObject);
                    quest.put("name", questName);
                    quest.put("editorTaskNum", editorTaskNum);
                    quest.put("saveTime", System.currentTimeMillis());
                    quest.put("startid", startId.getText());
                    try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(file), StandardCharsets.UTF_8))) {
                        writer.write(quest.toString());
                        Toast.makeText(MainActivity.this, "Сохранено в " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button exit = findViewById(R.id.EditBack);
        exit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                editorTasksObject = new JSONObject();
                editorTasks = new ArrayList<>();
                editorTaskNum = 0;
                questName = "";
                questStartId = "";
                setContentView(R.layout.quest_redactor);
                initRedactor();
            }
        });

        Button addNewRow = findViewById(R.id.EditAddNewRow);
        addNewRow.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                //editorTasks.add(new ArrayList<String>(Arrays.asList("Финал", "description", "ID: 4", "Переход: 4")));
                //adapter.notifyDataSetChanged();
                questName = editText.getText().toString();
                questStartId = startId.getText().toString();
                editorTaskNum++;
                setContentView(R.layout.editor_choose_type);
                initEditorChooseType();
            }
        });
        // set up the RecyclerView

        adapter = new MyRecyclerViewAdapter(this, editorTasks);
        adapter.setClickListener(this::onItemClick);
        recyclerView.setAdapter(adapter);


    }

    public void onItemClick(View view, int position) {
        try {
            String type = adapter.getItem(position).get(1);
            //Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();
            switch (type) {
                case "answerInput":
                    setContentView(R.layout.editor_choose_layout_1);
                    initEditorChooseLayout1Edit(Integer.parseInt(adapter.getItem(position).get(4)), position);
                    break;
                case "textInput":
                    setContentView(R.layout.editor_choose_layout_2);
                    initEditorChooseLayout2Edit(Integer.parseInt(adapter.getItem(position).get(4)), position);
                    break;
                case "slides":
                    editor3SlideObjects = new JSONObject();
                    editor3SlidesList = new ArrayList<>();
                    JSONObject textAnswerData = (JSONObject) editorTasksObject.get(Integer.toString(Integer.parseInt(adapter.getItem(position).get(4))));
                    try {
                        editor3SlideObjects = (JSONObject) textAnswerData.get("data");
                        JSONArray array = editor3SlideObjects.names();
                        ArrayList<Integer> arrInt = new ArrayList<>();
                        for (int i = 0; i < Objects.requireNonNull(array).length(); i++) {
                            arrInt.add(Integer.parseInt(((String) array.get(i)).substring(6)));
                        }
                        arrInt.sort(Comparator.naturalOrder());
                        for (int i = 0; i < arrInt.size(); i++) {
                            editor3SlidesList.add("Слайд " + arrInt.get(i));
                        }
                    } catch (Exception ignored) {
                    }
                    editor3SlidesNum = Integer.parseInt((String) textAnswerData.get("editorTaskNum"));
                    editor3SlidesIsEditing = true;
                    editorSlidesPosition = position;
                    editorSlidesTaskNum = Integer.parseInt(adapter.getItem(position).get(4));
                    editor3IdTo = textAnswerData.get("idto").toString();
                    setContentView(R.layout.editor_choose_layout_3);
                    initEditorChooseLayout3(Integer.parseInt(adapter.getItem(position).get(4)), position);
                    break;
                case "mapFlag":
                    setContentView(R.layout.editor_choose_layout_4);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainerView, new MapFragment()).commit();
                    initEditorChooseLayout4Edit(Integer.parseInt(adapter.getItem(position).get(4)), position);
                    break;

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initEditorChooseLayout1() {
        LinearLayout layout = findViewById(R.id.editChooseLayout1ButtonCase);
        List<View> lastView = new ArrayList<>();
        List<EditText> lastTextName = new ArrayList<>();
        List<EditText> lastTextIdTo = new ArrayList<>();
        TextView editChooseLayout1IDPage = findViewById(R.id.editChooseLayout1IDPage);
        editChooseLayout1IDPage.setText("ID текущего задания: " + editorTaskNum);
        EditText editChooseLayout1Description = findViewById(R.id.editChooseLayout1Description);


        EditText editChooseLayout1ButtonText1 = findViewById(R.id.editChooseLayout1ButtonText1);
        lastTextName.add(editChooseLayout1ButtonText1);

        EditText editChooseLayout1ButtonIDTO1 = findViewById(R.id.editChooseLayout1ButtonIDTO1);
        lastTextIdTo.add(editChooseLayout1ButtonIDTO1);

        Button removeButton = findViewById(R.id.editChooseLayout1RemoveButton);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (lastView.size() == 0) return;
                    layout.removeView(lastView.get(lastView.size() - 1));
                    lastView.remove(lastView.size() - 1);
                    lastTextName.remove(lastTextName.size() - 1);
                    lastTextIdTo.remove(lastTextIdTo.size() - 1);
                } catch (Exception ignored) {
                }
            }
        });

        Button addButton = findViewById(R.id.editChooseLayout1AddButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout textCase = new LinearLayout(MainActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                LinearLayout.LayoutParams params1 = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                params1.setMargins(40, 0, 40, 0);
                textCase.setOrientation(LinearLayout.HORIZONTAL);
                textCase.setLayoutParams(params);
                EditText buttonName = new EditText(MainActivity.this);
                EditText buttonIdTo = new EditText(MainActivity.this);
                buttonName.setLayoutParams(params1);
                buttonName.setHint("Текст кнопки");
                buttonName.setEms(10);
                buttonIdTo.setLayoutParams(params1);
                buttonIdTo.setHint("ID перехода");
                buttonIdTo.setEms(10);
                buttonIdTo.setInputType(InputType.TYPE_CLASS_NUMBER);
                textCase.addView(buttonName);
                textCase.addView(buttonIdTo);
                lastView.add(textCase);
                lastTextName.add(buttonName);
                lastTextIdTo.add(buttonIdTo);
                layout.addView(textCase);
            }
        });

        Button editChooseLayout1Remove = findViewById(R.id.editChooseLayout1Remove);
        editChooseLayout1Remove.setVisibility(View.GONE);
        Button editChooseLayout1Save = findViewById(R.id.editChooseLayout1Save);
        editChooseLayout1Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONArray buttonsArray = new JSONArray();
                JSONObject layoutObject = new JSONObject();
                List<Integer> to = new ArrayList<>();
                for (int i = 0; i < lastTextName.size(); i++) {
                    EditText buttonName = lastTextName.get(i);
                    EditText buttonIdTo = lastTextIdTo.get(i);
                    JSONObject obj = new JSONObject();
                    try {
                        if (buttonIdTo.getText().toString().length() == 0) {
                            Toast.makeText(MainActivity.this, "Необходимо указать ID перехода для всех кнопок", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        obj.put("name", buttonName.getText());
                        obj.put("idto", buttonIdTo.getText());
                        try {
                            to.add(Integer.parseInt(buttonIdTo.getText().toString()));
                        } catch (Exception ignored) {
                        }
                        ;
                        buttonsArray.put(obj);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    layoutObject.put("buttons", buttonsArray);
                    layoutObject.put("description", editChooseLayout1Description.getText());
                    layoutObject.put("id", editorTaskNum);
                    layoutObject.put("type", "answerInput");
                    editorTasksObject.put(Integer.toString(editorTaskNum), layoutObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                editorTasks.add(new ArrayList<String>(Arrays.asList("Выбор ответов", "answerInput", "ID: " + editorTaskNum, "Переход: " + to, "" + editorTaskNum)));
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });
        Button editChooseLayout1Exit = findViewById(R.id.editChooseLayout1Exit);
        editChooseLayout1Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });

    }

    private void initEditorChooseLayout1Edit(int taskNum, int position) throws JSONException {

        JSONObject textAnswerData = (JSONObject) editorTasksObject.get(Integer.toString(taskNum));

        LinearLayout layout = findViewById(R.id.editChooseLayout1ButtonCase);
        List<View> lastView = new ArrayList<>();
        List<EditText> lastTextName = new ArrayList<>();
        List<EditText> lastTextIdTo = new ArrayList<>();

        TextView editChooseLayout1IDPage = findViewById(R.id.editChooseLayout1IDPage);
        editChooseLayout1IDPage.setText("ID текущего задания: " + taskNum);
        EditText editChooseLayout1Description = findViewById(R.id.editChooseLayout1Description);
        editChooseLayout1Description.setText((CharSequence) textAnswerData.get("description"));

        EditText editChooseLayout1ButtonText1 = findViewById(R.id.editChooseLayout1ButtonText1);
        lastTextName.add(editChooseLayout1ButtonText1);

        EditText editChooseLayout1ButtonIDTO1 = findViewById(R.id.editChooseLayout1ButtonIDTO1);
        lastTextIdTo.add(editChooseLayout1ButtonIDTO1);

        JSONArray buttons = textAnswerData.getJSONArray("buttons");
        for (int i = 0; i < buttons.length(); i++) {
            JSONObject button = (JSONObject) buttons.get(i);

            if (i == 0) {
                editChooseLayout1ButtonText1.setText((CharSequence) button.get("name"));
                editChooseLayout1ButtonIDTO1.setText((CharSequence) button.get("idto"));
                continue;
            }

            LinearLayout textCase = new LinearLayout(MainActivity.this);
            LinearLayout.LayoutParams params = new LinearLayout
                    .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams params1 = new LinearLayout
                    .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            params1.setMargins(40, 0, 40, 0);
            textCase.setOrientation(LinearLayout.HORIZONTAL);
            textCase.setLayoutParams(params);
            EditText buttonName = new EditText(MainActivity.this);
            EditText buttonIdTo = new EditText(MainActivity.this);
            buttonName.setLayoutParams(params1);
            buttonName.setHint("Текст кнопки");
            buttonName.setText((CharSequence) button.get("name"));
            buttonName.setEms(10);
            buttonIdTo.setLayoutParams(params1);
            buttonIdTo.setText((CharSequence) button.get("idto"));
            buttonIdTo.setHint("ID перехода");
            buttonIdTo.setEms(10);
            textCase.addView(buttonName);
            textCase.addView(buttonIdTo);
            lastView.add(textCase);
            lastTextName.add(buttonName);
            lastTextIdTo.add(buttonIdTo);
            layout.addView(textCase);

        }

        Button removeButton = findViewById(R.id.editChooseLayout1RemoveButton);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (lastView.size() == 0) return;
                    layout.removeView(lastView.get(lastView.size() - 1));
                    lastView.remove(lastView.size() - 1);
                    lastTextName.remove(lastTextName.size() - 1);
                    lastTextIdTo.remove(lastTextIdTo.size() - 1);
                } catch (Exception ignored) {
                }
            }
        });

        Button addButton = findViewById(R.id.editChooseLayout1AddButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout textCase = new LinearLayout(MainActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                LinearLayout.LayoutParams params1 = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                params1.setMargins(40, 0, 40, 0);
                textCase.setOrientation(LinearLayout.HORIZONTAL);
                textCase.setLayoutParams(params);
                EditText buttonName = new EditText(MainActivity.this);
                EditText buttonIdTo = new EditText(MainActivity.this);
                buttonName.setLayoutParams(params1);
                buttonName.setHint("Текст кнопки");
                buttonName.setEms(10);
                buttonIdTo.setLayoutParams(params1);
                buttonIdTo.setHint("ID перехода");
                buttonIdTo.setEms(10);
                buttonIdTo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                buttonName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                textCase.addView(buttonName);
                textCase.addView(buttonIdTo);
                lastView.add(textCase);
                lastTextName.add(buttonName);
                lastTextIdTo.add(buttonIdTo);
                layout.addView(textCase);
            }
        });

        Button editChooseLayout1Remove = findViewById(R.id.editChooseLayout1Remove);
        editChooseLayout1Remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorTasks.remove(position);
                editorTasksObject.remove(Integer.toString(taskNum));
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });
        Button editChooseLayout1Save = findViewById(R.id.editChooseLayout1Save);
        editChooseLayout1Save.setText("Сохранить");
        editChooseLayout1Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONArray buttonsArray = new JSONArray();
                JSONObject layoutObject = new JSONObject();
                List<Integer> to = new ArrayList<>();
                for (int i = 0; i < lastTextName.size(); i++) {
                    EditText buttonName = lastTextName.get(i);
                    EditText buttonIdTo = lastTextIdTo.get(i);
                    JSONObject obj = new JSONObject();
                    try {
                        if (buttonIdTo.getText().toString().length() == 0) {
                            Toast.makeText(MainActivity.this, "Необходимо указать ID перехода для всех кнопок", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        obj.put("name", buttonName.getText());
                        obj.put("idto", buttonIdTo.getText());
                        try {
                            to.add(Integer.parseInt(buttonIdTo.getText().toString()));
                        } catch (Exception ignored) {
                        }
                        ;
                        buttonsArray.put(obj);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    layoutObject.put("buttons", buttonsArray);
                    layoutObject.put("id", taskNum);
                    layoutObject.put("type", "answerInput");
                    layoutObject.put("description", editChooseLayout1Description.getText());
                    editorTasksObject.remove(Integer.toString(taskNum));
                    editorTasksObject.put(Integer.toString(taskNum), layoutObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                editorTasks.set(position, new ArrayList<String>(Arrays.asList("Выбор ответов", "answerInput", "ID: " + taskNum, "Переход: " + to, "" + taskNum)));
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });
        Button editChooseLayout1Exit = findViewById(R.id.editChooseLayout1Exit);
        editChooseLayout1Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void initEditorChooseLayout2() {
        TextView editChooseLayout2IDPage = findViewById(R.id.editChooseLayout2IDPage);
        editChooseLayout2IDPage.setText("ID текущего задания: " + editorTaskNum);

        EditText editChooseLayout2IdTo = findViewById(R.id.editChooseLayout2IdTo);

        EditText editChooseLayout2Description = findViewById(R.id.editChooseLayout2Description);

        EditText editChooseLayout2Hint1 = findViewById(R.id.editChooseLayout2Hint1);
        EditText editChooseLayout2Hint2 = findViewById(R.id.editChooseLayout2Hint2);

        EditText editChooseLayout2Time1 = findViewById(R.id.editChooseLayout2Time1);
        EditText editChooseLayout2Time2 = findViewById(R.id.editChooseLayout2Time2);

        EditText editChooseLayout2Answer = findViewById(R.id.editChooseLayout2Answer);

        Button editChooseLayout2Remove = findViewById(R.id.editChooseLayout2Remove);
        editChooseLayout2Remove.setVisibility(View.GONE);
        Button editChooseLayout2Save = findViewById(R.id.editChooseLayout2Save);
        editChooseLayout2Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (editChooseLayout2IdTo.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "Необходимо указать ID перехода", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (editChooseLayout2Time1.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "Необходимо врямя для первой подсказки", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (editChooseLayout2Time2.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "Необходимо врямя для второй подсказки", Toast.LENGTH_SHORT).show();
                    return;
                }

                JSONObject obj = new JSONObject();
                try {
                    obj.put("description", editChooseLayout2Description.getText());
                    obj.put("hint1", editChooseLayout2Hint1.getText());
                    obj.put("hint2", editChooseLayout2Hint2.getText());
                    obj.put("time1", editChooseLayout2Time1.getText());
                    obj.put("time2", editChooseLayout2Time2.getText());
                    obj.put("answer", editChooseLayout2Answer.getText());
                    obj.put("idto", editChooseLayout2IdTo.getText());
                    obj.put("id", editorTaskNum);
                    obj.put("type", "textInput");
                    editorTasksObject.put(Integer.toString(editorTaskNum), obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                editorTasks.add(new ArrayList<String>(Arrays.asList("Текстовый ответ", "textInput", "ID: " + editorTaskNum, "Переход: " + editChooseLayout2IdTo.getText(), "" + editorTaskNum)));
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });
        Button editChooseLayout2Exit = findViewById(R.id.editChooseLayout2Exit);
        editChooseLayout2Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });

    }

    private void initEditorChooseLayout2Edit(int taskNum, int position) throws JSONException {

        JSONObject textAnswerData = (JSONObject) editorTasksObject.get(Integer.toString(taskNum));

        TextView editChooseLayout2IDPage = findViewById(R.id.editChooseLayout2IDPage);
        editChooseLayout2IDPage.setText("ID текущего задания: " + taskNum);

        EditText editChooseLayout2IdTo = findViewById(R.id.editChooseLayout2IdTo);
        editChooseLayout2IdTo.setText((CharSequence) textAnswerData.get("idto"));

        EditText editChooseLayout2Description = findViewById(R.id.editChooseLayout2Description);
        editChooseLayout2Description.setText((CharSequence) textAnswerData.get("description"));

        EditText editChooseLayout2Hint1 = findViewById(R.id.editChooseLayout2Hint1);
        editChooseLayout2Hint1.setText((CharSequence) textAnswerData.get("hint1"));
        EditText editChooseLayout2Hint2 = findViewById(R.id.editChooseLayout2Hint2);
        editChooseLayout2Hint2.setText((CharSequence) textAnswerData.get("hint2"));

        EditText editChooseLayout2Time1 = findViewById(R.id.editChooseLayout2Time1);
        editChooseLayout2Time1.setText((CharSequence) textAnswerData.get("time1"));
        EditText editChooseLayout2Time2 = findViewById(R.id.editChooseLayout2Time2);
        editChooseLayout2Time2.setText((CharSequence) textAnswerData.get("time2"));

        EditText editChooseLayout2Answer = findViewById(R.id.editChooseLayout2Answer);
        editChooseLayout2Answer.setText((CharSequence) textAnswerData.get("answer"));

        Button editChooseLayout2Remove = findViewById(R.id.editChooseLayout2Remove);
        editChooseLayout2Remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorTasks.remove(position);
                editorTasksObject.remove(Integer.toString(taskNum));
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });
        Button editChooseLayout2Save = findViewById(R.id.editChooseLayout2Save);
        editChooseLayout2Save.setText("Сохранить");
        editChooseLayout2Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (editChooseLayout2IdTo.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "Необходимо указать ID перехода", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (editChooseLayout2Time1.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "Необходимо врямя для первой подсказки", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (editChooseLayout2Time2.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "Необходимо врямя для второй подсказки", Toast.LENGTH_SHORT).show();
                    return;
                }

                JSONObject obj = new JSONObject();
                try {
                    obj.put("description", editChooseLayout2Description.getText());
                    obj.put("hint1", editChooseLayout2Hint1.getText());
                    obj.put("hint2", editChooseLayout2Hint2.getText());
                    obj.put("time1", editChooseLayout2Time1.getText());
                    obj.put("time2", editChooseLayout2Time2.getText());
                    obj.put("answer", editChooseLayout2Answer.getText());
                    obj.put("idto", editChooseLayout2IdTo.getText());
                    obj.put("id", taskNum);
                    obj.put("type", "textInput");
                    editorTasksObject.remove(Integer.toString(taskNum));
                    editorTasksObject.put(Integer.toString(taskNum), obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                editorTasks.set(position, new ArrayList<String>(Arrays.asList("Текстовый ответ", "textInput", "ID: " + taskNum, "Переход: " + editChooseLayout2IdTo.getText(), "" + taskNum)));
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });
        Button editChooseLayout2Exit = findViewById(R.id.editChooseLayout2Exit);
        editChooseLayout2Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });

    }

    private ArrayList<String> editor3SlidesList;
    private ArrayAdapter<String> editor3SlidesAdapter;
    private int editor3SlidesNum = 0;
    private String editor3IdTo = "0";
    private boolean editor3SlidesIsEditing = false;
    JSONObject editor3SlideObjects = new JSONObject();

    private void initEditorChooseLayout3(int taskNum, int position) {
        TextView textView = findViewById(R.id.editChooseLayout3Id);
        if (!editor3SlidesIsEditing) textView.setText("ID текущего задания: " + editorTaskNum);
        else textView.setText("ID текущего задания: " + editorSlidesTaskNum);

        ListView listView = findViewById(R.id.editChooseLayout3List);
        editor3SlidesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, editor3SlidesList);
        // Привяжем массив через адаптер к ListView
        listView.setAdapter(editor3SlidesAdapter);

        EditText editText = findViewById(R.id.editChooseLayout3IdTo);
        editText.setText(editor3IdTo);

        Button editChooseLayout3Save = findViewById(R.id.editChooseLayout3ButtonSave);
        if (!editor3SlidesIsEditing) editChooseLayout3Save.setText("Создать");
        editChooseLayout3Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (editor3SlidesList.size() == 0) {
                    Toast.makeText(MainActivity.this, "Необходим минимум 1 слайд", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (editText.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "Необходимо указать ID перехода", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject obj = new JSONObject();
                try {
                    obj.put("type", "slides");
                    obj.put("data", editor3SlideObjects);
                    obj.put("editorTaskNum", Integer.toString(editor3SlidesNum));
                    obj.put("idto", editText.getText());
                    if (!editor3SlidesIsEditing) obj.put("id", editorTaskNum);
                    else obj.put("id", editorSlidesTaskNum);
                    obj.put("type", "slides");
                    if (editor3SlidesIsEditing) {
                        editorTasksObject.remove(Integer.toString(editorSlidesTaskNum));
                        editorTasksObject.put(Integer.toString(editorSlidesTaskNum), obj);
                    } else {
                        editorTasksObject.put(Integer.toString(editorTaskNum), obj);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (!editor3SlidesIsEditing)
                    editorTasks.add(new ArrayList<String>(Arrays.asList("Слайды", "slides", "ID: " + editorTaskNum, "Переход: " + editText.getText(), "" + editorTaskNum)));
                else {
                    editorTasks.set(editorSlidesPosition, new ArrayList<String>(Arrays.asList("Слайды", "slides", "ID: " + editorSlidesTaskNum, "Переход: " + editText.getText(), "" + editorSlidesTaskNum)));
                }
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });

        Button editChooseLayout3ButtonRemove = findViewById(R.id.editChooseLayout3ButtonRemove);
        if (!editor3SlidesIsEditing) editChooseLayout3ButtonRemove.setVisibility(View.GONE);
        editChooseLayout3ButtonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorTasks.remove(editorSlidesPosition);
                editorTasksObject.remove(Integer.toString(editorSlidesTaskNum));
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });
        Button editChooseLayout3Exit = findViewById(R.id.editChooseLayout3ButtonExit);
        editChooseLayout3Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });
        Button editChooseLayout3Add = findViewById(R.id.editChooseLayout3ButtonAdd);
        editChooseLayout3Add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor3IdTo = editText.getText().toString();
                editor3SlidesNum++;
                setContentView(R.layout.editor_layout_3_slide_editor);
                initEditorChooseLayoutSlideEditor();
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                    long id) {
                setContentView(R.layout.editor_layout_3_slide_editor);
                try {
                    initEditorChooseLayoutSlideEditorEdit(((TextView) itemClicked).getText().toString(), position);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    LinearLayout editorChooseLayoutSlideEditorSlide;

    private void initEditorChooseLayoutSlideEditorEdit(String slideName, int position) throws JSONException {
        editorChooseLayoutSlideEditorSlide = findViewById(R.id.editChooseLayout3EditSlideSlide);
        JSONArray slideData = (JSONArray) editor3SlideObjects.get(slideName);
        for (int i = 0; i < slideData.length(); i++) {
            JSONObject object = (JSONObject) slideData.get(i);
            if (object.get("type").equals("text")) {
                EditText editText = new EditText(MainActivity.this);
                Button deleteButton = new Button(MainActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                editText.setLayoutParams(params);
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                editText.setText((CharSequence) object.get("data"));
                deleteButton.setLayoutParams(params);
                deleteButton.setText("Удалить текст выше");
                editorChooseLayoutSlideEditorSlide.addView(editText);
                editorChooseLayoutSlideEditorSlide.addView(deleteButton);

                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        editorChooseLayoutSlideEditorSlide.removeView(editText);
                        editorChooseLayoutSlideEditorSlide.removeView(deleteButton);
                    }
                });
            }
            if (object.get("type").equals("image")) {
                String encodedImage = (String) object.get("data");
                byte[] decodedString = Base64.decode(encodedImage, DEFAULT);
                ImageView imageView = new ImageView(MainActivity.this);
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setAdjustViewBounds(true);
                Button deleteButton = new Button(MainActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                deleteButton.setLayoutParams(params);
                deleteButton.setText("Удалить картинку выше");

                editorChooseLayoutSlideEditorSlide.addView(imageView);
                editorChooseLayoutSlideEditorSlide.addView(deleteButton);

                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        editorChooseLayoutSlideEditorSlide.removeView(imageView);
                        editorChooseLayoutSlideEditorSlide.removeView(deleteButton);
                    }
                });
            }
        }
        Button editChooseLayout3EditSlideAddText = findViewById(R.id.editChooseLayout3EditSlideAddText);
        editChooseLayout3EditSlideAddText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = new EditText(MainActivity.this);
                Button deleteButton = new Button(MainActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                editText.setLayoutParams(params);
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                editText.setHint("Введите текст");
                deleteButton.setLayoutParams(params);
                deleteButton.setText("Удалить текст выше");
                editorChooseLayoutSlideEditorSlide.addView(editText);
                editorChooseLayoutSlideEditorSlide.addView(deleteButton);

                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        editorChooseLayoutSlideEditorSlide.removeView(editText);
                        editorChooseLayoutSlideEditorSlide.removeView(deleteButton);
                    }
                });


            }
        });
        Button editChooseLayout3EditSlideAddPic = findViewById(R.id.editChooseLayout3EditSlideAddPic);
        editChooseLayout3EditSlideAddPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                someActivityResultLauncher.launch(i);

            }
        });
        Button editChooseLayout3EditSlideSave = findViewById(R.id.editChooseLayout3EditSlideSave);
        editChooseLayout3EditSlideSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                JSONArray slideData = new JSONArray();
                final int childCount = editorChooseLayoutSlideEditorSlide.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View v = editorChooseLayoutSlideEditorSlide.getChildAt(i);
                    if (v instanceof TextView && !(v instanceof Button)) {
                        TextView textView = (TextView) v;
                        JSONObject text = new JSONObject();
                        try {
                            text.put("type", "text");
                            text.put("data", textView.getText());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        slideData.put(text);
                    }
                    if (v instanceof ImageView) {
                        ImageView imageView = (ImageView) v;
                        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                        Bitmap bitmap = drawable.getBitmap();
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        // compress Bitmap
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        // Initialize byte array
                        byte[] bytes = stream.toByteArray();
                        // get base64 encoded strin
                        String img_str = Base64.encodeToString(bytes, DEFAULT);
                        JSONObject img = new JSONObject();
                        try {
                            img.put("type", "image");
                            img.put("data", img_str);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        slideData.put(img);
                    }
                }
                try {
                    editor3SlideObjects.remove(slideName);
                    editor3SlideObjects.put(slideName, slideData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                setContentView(R.layout.editor_choose_layout_3);
                initEditorChooseLayout3(-1, -1);
            }
        });
        Button editChooseLayout3EditSlideDelete = findViewById(R.id.editChooseLayout3EditSlideDelete);
        editChooseLayout3EditSlideDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor3SlideObjects.remove(slideName);
                editor3SlidesList.remove(position);
                editor3SlidesAdapter.notifyDataSetChanged();
                setContentView(R.layout.editor_choose_layout_3);
                initEditorChooseLayout3(-1, -1);
            }
        });
        Button editChooseLayout3EditSlideExit = findViewById(R.id.editChooseLayout3EditSlideExit);
        editChooseLayout3EditSlideExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_choose_layout_3);
                initEditorChooseLayout3(-1, -1);
            }
        });

    }

    private void initEditorChooseLayoutSlideEditor() {


        // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed


        editorChooseLayoutSlideEditorSlide = findViewById(R.id.editChooseLayout3EditSlideSlide);

        Button editChooseLayout3EditSlideAddText = findViewById(R.id.editChooseLayout3EditSlideAddText);
        editChooseLayout3EditSlideAddText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = new EditText(MainActivity.this);
                Button deleteButton = new Button(MainActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                editText.setLayoutParams(params);
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                editText.setHint("Введите текст");
                deleteButton.setLayoutParams(params);
                deleteButton.setText("Удалить текст выше");
                editorChooseLayoutSlideEditorSlide.addView(editText);
                editorChooseLayoutSlideEditorSlide.addView(deleteButton);

                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        editorChooseLayoutSlideEditorSlide.removeView(editText);
                        editorChooseLayoutSlideEditorSlide.removeView(deleteButton);
                    }
                });


            }
        });
        Button editChooseLayout3EditSlideAddPic = findViewById(R.id.editChooseLayout3EditSlideAddPic);
        editChooseLayout3EditSlideAddPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                someActivityResultLauncher.launch(i);

            }
        });
        Button editChooseLayout3EditSlideSave = findViewById(R.id.editChooseLayout3EditSlideSave);
        editChooseLayout3EditSlideSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                JSONArray slideData = new JSONArray();
                final int childCount = editorChooseLayoutSlideEditorSlide.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View v = editorChooseLayoutSlideEditorSlide.getChildAt(i);
                    if (v instanceof TextView && !(v instanceof Button)) {
                        TextView textView = (TextView) v;
                        JSONObject text = new JSONObject();
                        try {
                            text.put("type", "text");
                            text.put("data", textView.getText());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        slideData.put(text);
                    }
                    if (v instanceof ImageView) {
                        ImageView imageView = (ImageView) v;
                        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                        Bitmap bitmap = drawable.getBitmap();
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        // compress Bitmap
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        // Initialize byte array
                        byte[] bytes = stream.toByteArray();
                        // get base64 encoded strin
                        String img_str = Base64.encodeToString(bytes, DEFAULT);
                        JSONObject img = new JSONObject();
                        try {
                            img.put("type", "image");
                            img.put("data", img_str);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        slideData.put(img);
                    }
                }
                try {
                    editor3SlidesList.add("Слайд " + editor3SlidesNum);
                    editor3SlidesAdapter.notifyDataSetChanged();
                    editor3SlideObjects.put("Слайд " + editor3SlidesNum, slideData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                setContentView(R.layout.editor_choose_layout_3);
                initEditorChooseLayout3(-1, -1);
            }
        });
        Button editChooseLayout3EditSlideDelete = findViewById(R.id.editChooseLayout3EditSlideDelete);
        editChooseLayout3EditSlideDelete.setVisibility(View.GONE);
        Button editChooseLayout3EditSlideExit = findViewById(R.id.editChooseLayout3EditSlideExit);
        editChooseLayout3EditSlideExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_choose_layout_3);
                initEditorChooseLayout3(-1, -1);
            }
        });

    }

    private void initEditorChooseLayout4() {
        EditText editChooseLayout4IdTo = findViewById(R.id.editChooseLayout4IdTo);
        EditText editChooseLayout4FlagName = findViewById(R.id.editChooseLayout4FlagName);
        TextView textView = findViewById(R.id.editChooseLayout4Id);
        textView.setText("ID текущего задания: " + editorTaskNum);
        Button editChooseLayout4Save = findViewById(R.id.editChooseLayout4Save);
        editChooseLayout4Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MapFragment.longitude != 0 && MapFragment.latitude != 0) {
                    JSONObject obj = new JSONObject();
                    try {
                        if (editChooseLayout4IdTo.getText().toString().length() == 0) {
                            Toast.makeText(MainActivity.this, "Необходимо указать ID перехода", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        obj.put("type", "mapFlag");
                        obj.put("longitude", MapFragment.longitude);
                        obj.put("latitude", MapFragment.latitude);
                        obj.put("idto", editChooseLayout4IdTo.getText());
                        obj.put("name", editChooseLayout4FlagName.getText());
                        obj.put("id", editorTaskNum);
                        obj.put("type", "mapFlag");
                        editorTasksObject.put(Integer.toString(editorTaskNum), obj);
                        editorTasks.add(new ArrayList<String>(Arrays.asList("Геолокация", "mapFlag", "ID: " + editorTaskNum, "Переход: " + editChooseLayout4IdTo.getText(), "" + editorTaskNum)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    setContentView(R.layout.editor_list);
                    initEditor();
                } else {
                    Toast.makeText(MainActivity.this, "Необходимо указать метку", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        Button editChooseLayout4Remove = findViewById(R.id.editChooseLayout4Remove);
        editChooseLayout4Remove.setVisibility(View.GONE);

        Button editChooseLayout4Exit = findViewById(R.id.editChooseLayout4Exit);
        editChooseLayout4Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });

    }

    private void initEditorChooseLayout4Edit(int taskNum, int position) throws JSONException {
        JSONObject textAnswerData = (JSONObject) editorTasksObject.get(Integer.toString(taskNum));

        TextView textView = findViewById(R.id.editChooseLayout4Id);
        textView.setText("ID текущего задания: " + taskNum);
        EditText editChooseLayout4FlagName = findViewById(R.id.editChooseLayout4FlagName);
        editChooseLayout4FlagName.setText((CharSequence) textAnswerData.get("name"));
        EditText editChooseLayout4IdTo = findViewById(R.id.editChooseLayout4IdTo);
        editChooseLayout4IdTo.setText((CharSequence) textAnswerData.get("idto"));
        MapFragment.isEditing = true;
        MapFragment.longitude = (double) textAnswerData.get("longitude");
        MapFragment.latitude = (double) textAnswerData.get("latitude");
        //MapFragment.updateMarker();

        Button editChooseLayout4Save = findViewById(R.id.editChooseLayout4Save);
        editChooseLayout4Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MapFragment.longitude != 0 && MapFragment.latitude != 0) {
                    JSONObject obj = new JSONObject();
                    try {
                        if (editChooseLayout4IdTo.getText().toString().length() == 0) {
                            Toast.makeText(MainActivity.this, "Необходимо указать ID перехода", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        obj.put("type", "mapFlag");
                        obj.put("longitude", MapFragment.longitude);
                        obj.put("latitude", MapFragment.latitude);
                        obj.put("idto", editChooseLayout4IdTo.getText());
                        obj.put("name", editChooseLayout4FlagName.getText());
                        obj.put("id", taskNum);
                        obj.put("type", "mapFlag");
                        editorTasksObject.remove(Integer.toString(taskNum));
                        editorTasksObject.put(Integer.toString(taskNum), obj);
                        editorTasks.set(position, new ArrayList<String>(Arrays.asList("Геолокация", "mapFlag", "ID: " + taskNum, "Переход: " + editChooseLayout4IdTo.getText(), "" + taskNum)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    setContentView(R.layout.editor_list);
                    initEditor();
                } else {
                    Toast.makeText(MainActivity.this, "Необходимо указать метку", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        Button editChooseLayout4Remove = findViewById(R.id.editChooseLayout4Remove);
        editChooseLayout4Remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorTasks.remove(position);
                editorTasksObject.remove(Integer.toString(taskNum));
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });

        Button editChooseLayout4Exit = findViewById(R.id.editChooseLayout4Exit);
        editChooseLayout4Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });

    }

    private void initEditorChooseType() {
        Button editorChooseType1 = findViewById(R.id.EditorChooseType1);
        editorChooseType1.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_choose_layout_1);
                initEditorChooseLayout1();
            }
        });
        Button editorChooseType2 = findViewById(R.id.EditorChooseType2);
        editorChooseType2.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_choose_layout_2);
                initEditorChooseLayout2();
            }
        });
        Button editorChooseType3 = findViewById(R.id.EditorChooseType3);
        editorChooseType3.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                editor3SlideObjects = new JSONObject();
                editor3SlidesList = new ArrayList<>();
                editor3SlidesNum = 0;
                editor3SlidesIsEditing = false;
                editorSlidesTaskNum = 0;
                editorSlidesPosition = 0;
                editor3IdTo = "";
                setContentView(R.layout.editor_choose_layout_3);
                initEditorChooseLayout3(-1, -1);
            }
        });
        Button editorChooseType4 = findViewById(R.id.EditorChooseType4);
        editorChooseType4.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                MapFragment.isEditing = false;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerView, new MapFragment()).commit();
                setContentView(R.layout.editor_choose_layout_4);
                initEditorChooseLayout4();
            }
        });
        Button editorChooseTypeExit = findViewById(R.id.EditorChooseTypeExit);
        editorChooseTypeExit.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                setContentView(R.layout.editor_list);
                initEditor();
            }
        });
    }

    private void initPlayer() {
        chronologyMap = new JSONObject();
        chronologyList = new ArrayList<>();
        chronologyStep = 0;
        chronologyPosition = 0;
        ArrayList<String> redactorList = new ArrayList<>();
        ArrayAdapter<String> redactorAdapter;
        ListView listView = findViewById(R.id.playerList);


        Button editChooseLayout3ButtonExit = findViewById(R.id.playerQuestExit);
        editChooseLayout3ButtonExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.activity_main);
                initHome();
            }
        });

        String path = Environment.getExternalStorageDirectory() + "/CubesQuest";
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (int i = 0; i < Objects.requireNonNull(files).length; i++) {
            if (files[i].getAbsolutePath().substring(files[i].getAbsolutePath().lastIndexOf(".")).equals(".quest")) {
                redactorList.add(files[i].getName());
                //Log.d("Files", files[i].getName() + " Added!");
            }
            //Log.d("Files", "FileName:" + files[i].getName());
        }
        redactorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, redactorList);
        listView.setAdapter(redactorAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                    long id) {
                String json = readFromFile(MainActivity.this, ((TextView) itemClicked).getText().toString());
                try {
                    JSONObject rawobj = new JSONObject(json);
                    editorTasksObject = (JSONObject) rawobj.get("data");
                    int startId = Integer.parseInt(rawobj.get("startid").toString());
                    runPlayer(startId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initFinish(){
        Button exit = findViewById(R.id.finishExit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.quest_player);
                initPlayer();
            }
        });
    }

    private void runPlayerView(int playerId) throws JSONException {


        JSONObject obj;
        try {
            obj = (JSONObject) editorTasksObject.get(Integer.toString(playerId));
        } catch (Exception e) {
            //TODO finish
        }

        obj = (JSONObject) editorTasksObject.get(Integer.toString(playerId));
        if (obj.get("type").equals("answerInput")) {
            JSONArray buttonsArray = (JSONArray) obj.get("buttons");
            List<String> names = new ArrayList<>();
            List<Integer> idsto = new ArrayList<>();
            for (int j = 0; j < buttonsArray.length(); j++) {
                idsto.add(Integer.parseInt(((JSONObject) buttonsArray.get(j)).get("idto").toString()));
                names.add(((JSONObject) buttonsArray.get(j)).get("name").toString());
            }
            setContentView(R.layout.player_layout_1);
            initPlayerLayout1View(names, idsto, obj.get("description").toString());
        } else if (obj.get("type").equals("textInput")) {
            setContentView(R.layout.player_layout_2);
            initPlayerLayout2View(obj.get("description").toString(),
                    obj.get("answer").toString(),
                    obj.get("hint1").toString(),
                    obj.get("hint2").toString(),
                    Integer.parseInt(obj.get("time1").toString()),
                    Integer.parseInt(obj.get("time2").toString()),
                    Integer.parseInt(obj.get("idto").toString()));
        } else if (obj.get("type").equals("slides")) {
            JSONObject slideObj = (JSONObject) obj.get("data");
            setContentView(R.layout.player_layout_3);
            initPlayerLayout3View(slideObj, 0, Integer.parseInt(obj.get("idto").toString()));
            //editorTasks.add(new ArrayList<String>(Arrays.asList("Слайды", "slides", "ID: " + obj.get("id"), "Переход: " + obj.get("idto"), "" + obj.get("id"))));
        } else {
            MapPlayerFragment.latitude = (double) obj.get("latitude");
            MapPlayerFragment.longitude = (double) obj.get("longitude");
            MapPlayerFragment.idto = Integer.parseInt(obj.get("idto").toString());
            MapPlayerFragment.name = obj.get("name").toString();
            MapPlayerFragment.distance = 100;
            MapPlayerFragment.isNew = true;
            setContentView(R.layout.player_layout_4);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView2, new MapPlayerFragment()).commit();
            initPlayerLayout4View();
        }

    }
    private void runPlayer(int playerId) throws JSONException {


        JSONObject obj;
        try {
            obj = (JSONObject) editorTasksObject.get(Integer.toString(playerId));
        } catch (Exception e) {
            setContentView(R.layout.finish_layout);
            initFinish();
            return;
        }
        chronologyStep++;
        chronologyMap.put("Шаг " + chronologyStep, playerId);
        chronologyList.add("Шаг " + chronologyStep);

        obj = (JSONObject) editorTasksObject.get(Integer.toString(playerId));
        if (obj.get("type").equals("answerInput")) {
            JSONArray buttonsArray = (JSONArray) obj.get("buttons");
            List<String> names = new ArrayList<>();
            List<Integer> idsto = new ArrayList<>();
            for (int j = 0; j < buttonsArray.length(); j++) {
                idsto.add(Integer.parseInt(((JSONObject) buttonsArray.get(j)).get("idto").toString()));
                names.add(((JSONObject) buttonsArray.get(j)).get("name").toString());
            }
            setContentView(R.layout.player_layout_1);
            initPlayerLayout1(names, idsto, obj.get("description").toString());
            //editorTasks.add(new ArrayList<String>(Arrays.asList("Выбор ответов", "answerInput", "ID: " + obj.get("id"), "Переход: " + idsto.toString(), "" + obj.get("id"))));
        } else if (obj.get("type").equals("textInput")) {
            setContentView(R.layout.player_layout_2);
            initPlayerLayout2(obj.get("description").toString(),
                    obj.get("answer").toString(),
                    obj.get("hint1").toString(),
                    obj.get("hint2").toString(),
                    Integer.parseInt(obj.get("time1").toString()),
                    Integer.parseInt(obj.get("time2").toString()),
                    Integer.parseInt(obj.get("idto").toString()));
        } else if (obj.get("type").equals("slides")) {
            JSONObject slideObj = (JSONObject) obj.get("data");
            setContentView(R.layout.player_layout_3);
            initPlayerLayout3(slideObj, 0, Integer.parseInt(obj.get("idto").toString()));
            //editorTasks.add(new ArrayList<String>(Arrays.asList("Слайды", "slides", "ID: " + obj.get("id"), "Переход: " + obj.get("idto"), "" + obj.get("id"))));
        } else {
            MapPlayerFragment.latitude = (double) obj.get("latitude");
            MapPlayerFragment.longitude = (double) obj.get("longitude");
            MapPlayerFragment.idto = Integer.parseInt(obj.get("idto").toString());
            MapPlayerFragment.name = obj.get("name").toString();
            MapPlayerFragment.distance = 100;
            MapPlayerFragment.isNew = true;
            setContentView(R.layout.player_layout_4);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView2, new MapPlayerFragment()).commit();
            initPlayerLayout4();
        }

    }

    JSONObject chronologyMap = new JSONObject();
    ArrayList<String> chronologyList = new ArrayList<>();
    int chronologyStep = 0;
    int chronologyPosition = 0;
    private void initChronology(){
        ArrayAdapter<String> redactorAdapter;
        ListView listView = findViewById(R.id.chronology);


        Button editChooseLayout3ButtonExit = findViewById(R.id.playerQuestExit);
        editChooseLayout3ButtonExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String curname = chronologyList.get(chronologyList.size() - 1);
                    int curid = (int) chronologyMap.get(curname);
                    chronologyMap.remove(curname);
                    chronologyList.remove(chronologyList.size() - 1);
                    chronologyStep--;
                    runPlayer(curid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        redactorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chronologyList);
        listView.setAdapter(redactorAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                    long id) {
                try {
                    int questId = (int) chronologyMap.get(((TextView) itemClicked).getText().toString());
                    if(position == chronologyList.size() - 1){
                        Toast.makeText(MainActivity.this, "Вы еще не прошли этот шаг", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    chronologyPosition = position;
                    runPlayerView(questId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initPlayerLayout1View(List<String> names, List<Integer> idsto, String question) {
        LinearLayout buttonCase = findViewById(R.id.playerLayout1ButtonCase);
        LinearLayout.LayoutParams params = new LinearLayout
                .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        for (int i = 0; i < names.size(); i++) {
            MaterialButton button = new MaterialButton(MainActivity.this);
            button.setText(names.get(i));
            button.setLayoutParams(params);

            try {
                if(idsto.get(i) == (int) chronologyMap.get(chronologyList.get(chronologyPosition + 1))) button.setEnabled(true);
                else button.setEnabled(false);

                buttonCase.addView(button);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        TextView text = findViewById(R.id.playerLayout1Question);
        text.setText(question);
        Button exit = findViewById(R.id.playerLayout1Exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String curname = chronologyList.get(chronologyList.size() - 1);
                    int curid = (int) chronologyMap.get(curname);
                    chronologyMap.remove(curname);
                    chronologyList.remove(chronologyList.size() - 1);
                    chronologyStep--;
                    runPlayer(curid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        Button chronology = findViewById(R.id.playerLayout1Chronology);
        chronology.setVisibility(View.GONE);
    }
    private void initPlayerLayout1(List<String> names, List<Integer> idsto, String question) {
        LinearLayout buttonCase = findViewById(R.id.playerLayout1ButtonCase);
        LinearLayout.LayoutParams params = new LinearLayout
                .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        for (int i = 0; i < names.size(); i++) {
            MaterialButton button = new MaterialButton(MainActivity.this);
            button.setText(names.get(i));
            button.setLayoutParams(params);
            int finalI = i;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        runPlayer(idsto.get(finalI));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            buttonCase.addView(button);
        }
        TextView text = findViewById(R.id.playerLayout1Question);
        text.setText(question);
        Button exit = findViewById(R.id.playerLayout1Exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.quest_player);
                initPlayer();
            }
        });
        Button chronology = findViewById(R.id.playerLayout1Chronology);
        chronology.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.chronology_layout);
                initChronology();
            }
        });
    }

    private class PlayerLayout2Hints extends AsyncTask<Void, Integer, Void> {

        TextView hintInfo;
        TextView hintText;
        String hint1, hint2;
        int hintTime1, hintTime2;
        long startTime;
        long timeNow = 0;
        int cnt = 0;
        private boolean playerLayout2HintsIsRunning = true;

        public PlayerLayout2Hints(TextView hintText, TextView hintInfo, String hint1, String hint2, int hintTime1, int hintTime2) {
            super();
            this.hintInfo = hintInfo;
            this.hintTime1 = hintTime1;
            this.hintTime2 = hintTime2;
            this.hintText = hintText;
            this.hint1 = hint1;
            this.hint2 = hint2;
            startTime = System.currentTimeMillis();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (playerLayout2HintsIsRunning) {
                if (cnt >= 2) return null;
                if (System.currentTimeMillis() - timeNow > 1000) {
                    onProgressUpdate((int) (hintTime1 - (System.currentTimeMillis() - startTime) / 1000));
                    timeNow = System.currentTimeMillis();
                }
            }
            return null;
        }

        protected void onProgressUpdate(Integer progress) {
            if ((System.currentTimeMillis() - startTime) / 1000 > hintTime1) {
                hintText.setText(hint1);
                startTime = System.currentTimeMillis();
                hintTime1 = hintTime2;
                hint1 = hint2;
                cnt++;
                if (cnt == 1) return;
            }
            if (cnt < 2) {
                hintInfo.setText("Подсказка через " + progress + "c");
            } else {
                hintInfo.setText("Подсказки закончились(");
            }
        }

    }

    private void initPlayerLayout2View(String question, String answer, String hint1, String hint2, int time1, int time2, int idto) {
        TextView questionView = findViewById(R.id.playerLayout2Question);
        questionView.setText(question);

        TextView hintText = findViewById(R.id.playerLayout2hintText);
        hintText.setText(hint2);
        TextView hintInfo = findViewById(R.id.playerLayout2HintInfo);
        hintInfo.setVisibility(View.GONE);

        EditText editText = findViewById(R.id.playerLayout2Answer);
        editText.setText(answer);
        editText.setEnabled(false);

        Button enter = findViewById(R.id.playerLayout2EnterButton);
        enter.setEnabled(false);

        Button exit = findViewById(R.id.playerLayout2Exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String curname = chronologyList.get(chronologyList.size() - 1);
                    int curid = (int) chronologyMap.get(curname);
                    chronologyMap.remove(curname);
                    chronologyList.remove(chronologyList.size() - 1);
                    chronologyStep--;
                    runPlayer(curid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        Button chronology = findViewById(R.id.playerLayout2Chronology);
        chronology.setVisibility(View.GONE);
    }
    private void initPlayerLayout2(String question, String answer, String hint1, String hint2, int time1, int time2, int idto) {
        TextView questionView = findViewById(R.id.playerLayout2Question);
        questionView.setText(question);

        TextView hintText = findViewById(R.id.playerLayout2hintText);
        TextView hintInfo = findViewById(R.id.playerLayout2HintInfo);
        PlayerLayout2Hints playerLayout2Hints = new PlayerLayout2Hints(hintText, hintInfo, hint1, hint2, time1, time2);
        playerLayout2Hints.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        EditText editText = findViewById(R.id.playerLayout2Answer);

        Button enter = findViewById(R.id.playerLayout2EnterButton);
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (editText.getText().toString().equals(answer)) {
                    try {
                        playerLayout2Hints.playerLayout2HintsIsRunning = false;
                        runPlayer(idto);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Неправильный ответ", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button exit = findViewById(R.id.playerLayout2Exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.quest_player);
                initPlayer();
            }
        });
        Button chronology = findViewById(R.id.playerLayout2Chronology);
        chronology.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.chronology_layout);
                initChronology();
            }
        });
    }

    private void initPlayerLayout3View(JSONObject slideObj, int slideNow, int idto) throws JSONException {
        JSONArray array = slideObj.names();
        LinearLayout slideCase = findViewById(R.id.playerLayout3SlideCase);
        List<String> slideNames = new ArrayList<>();
        ArrayList<Integer> arrInt = new ArrayList<>();
        for (int i = 0; i < Objects.requireNonNull(array).length(); i++) {
            arrInt.add(Integer.parseInt(((String) array.get(i)).substring(6)));
        }
        arrInt.sort(Comparator.naturalOrder());
        for (int i = 0; i < arrInt.size(); i++) {
            slideNames.add("Слайд " + arrInt.get(i));
        }
        String slideName = slideNames.get(slideNow);

        JSONArray slideData = (JSONArray) slideObj.get(slideName);
        for (int i = 0; i < slideData.length(); i++) {
            JSONObject object = (JSONObject) slideData.get(i);
            if (object.get("type").equals("text")) {
                MaterialTextView editText = new MaterialTextView(MainActivity.this);
                Button deleteButton = new Button(MainActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                editText.setLayoutParams(params);
                editText.setTextAppearance(this, com.google.android.material.R.style.Base_TextAppearance_AppCompat_Large);
                editText.setText((CharSequence) object.get("data"));
                editText.setGravity(Gravity.CENTER);
                slideCase.addView(editText);
            }
            if (object.get("type").equals("image")) {
                String encodedImage = (String) object.get("data");
                byte[] decodedString = Base64.decode(encodedImage, DEFAULT);
                ImageView imageView = new ImageView(MainActivity.this);
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setAdjustViewBounds(true);
                slideCase.addView(imageView);
            }
        }

        TextView playerLayout3SlideInfo = findViewById(R.id.playerLayout3SlideInfo);
        playerLayout3SlideInfo.setText("Слайд " + (slideNow + 1) + " из " + (slideNames.size()));

        Button nextSlide = findViewById(R.id.playerLayout3Next);
        if (slideNow == slideNames.size() - 1) nextSlide.setVisibility(View.GONE);
        else nextSlide.setVisibility(View.VISIBLE);
        nextSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    slideCase.removeAllViews();
                    initPlayerLayout3View(slideObj, slideNow + 1, idto);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        Button backSlide = findViewById(R.id.playerLayout3Back);
        if (slideNow == 0) backSlide.setEnabled(false);
        else backSlide.setEnabled(true);
        backSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    slideCase.removeAllViews();
                    initPlayerLayout3View(slideObj, slideNow - 1, idto);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button exit = findViewById(R.id.playerLayout3Exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String curname = chronologyList.get(chronologyList.size() - 1);
                    int curid = (int) chronologyMap.get(curname);
                    chronologyMap.remove(curname);
                    chronologyList.remove(chronologyList.size() - 1);
                    chronologyStep--;
                    runPlayer(curid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        Button chronology = findViewById(R.id.playerLayout3Chronology);
        chronology.setVisibility(View.GONE);
    }
    private void initPlayerLayout3(JSONObject slideObj, int slideNow, int idto) throws JSONException {
        JSONArray array = slideObj.names();
        LinearLayout slideCase = findViewById(R.id.playerLayout3SlideCase);
        List<String> slideNames = new ArrayList<>();
        ArrayList<Integer> arrInt = new ArrayList<>();
        for (int i = 0; i < Objects.requireNonNull(array).length(); i++) {
            arrInt.add(Integer.parseInt(((String) array.get(i)).substring(6)));
        }
        arrInt.sort(Comparator.naturalOrder());
        for (int i = 0; i < arrInt.size(); i++) {
            slideNames.add("Слайд " + arrInt.get(i));
        }
        String slideName = slideNames.get(slideNow);

        JSONArray slideData = (JSONArray) slideObj.get(slideName);
        for (int i = 0; i < slideData.length(); i++) {
            JSONObject object = (JSONObject) slideData.get(i);
            if (object.get("type").equals("text")) {
                MaterialTextView editText = new MaterialTextView(MainActivity.this);
                Button deleteButton = new Button(MainActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                editText.setLayoutParams(params);
                editText.setTextAppearance(this, com.google.android.material.R.style.Base_TextAppearance_AppCompat_Large);
                editText.setText((CharSequence) object.get("data"));
                editText.setGravity(Gravity.CENTER);
                slideCase.addView(editText);
            }
            if (object.get("type").equals("image")) {
                String encodedImage = (String) object.get("data");
                byte[] decodedString = Base64.decode(encodedImage, DEFAULT);
                ImageView imageView = new ImageView(MainActivity.this);
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setAdjustViewBounds(true);
                slideCase.addView(imageView);
            }
        }

        TextView playerLayout3SlideInfo = findViewById(R.id.playerLayout3SlideInfo);
        playerLayout3SlideInfo.setText("Слайд " + (slideNow + 1) + " из " + (slideNames.size()));

        Button nextSlide = findViewById(R.id.playerLayout3Next);
        if (slideNow == slideNames.size() - 1) nextSlide.setText("Дальше");
        else nextSlide.setText("Следующий слайд");
        nextSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    slideCase.removeAllViews();
                    if (slideNow == slideNames.size() - 1) {
                        runPlayer(idto);
                    } else {
                        initPlayerLayout3(slideObj, slideNow + 1, idto);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        Button backSlide = findViewById(R.id.playerLayout3Back);
        if (slideNow == 0) backSlide.setEnabled(false);
        else backSlide.setEnabled(true);
        backSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    slideCase.removeAllViews();
                    initPlayerLayout3(slideObj, slideNow - 1, idto);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button exit = findViewById(R.id.playerLayout3Exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.quest_player);
                initPlayer();
            }
        });
        Button chronology = findViewById(R.id.playerLayout3Chronology);
        chronology.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.chronology_layout);
                initChronology();
            }
        });


    }

    public void initPlayerLayout4View() {
        TextView playerLayout4dist = findViewById(R.id.playerLayout4dist);
        playerLayout4dist.setText("Вы дошли до цели");
        Button exit = findViewById(R.id.playerLayout4Exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapPlayerFragment.isNew = false;
                try {
                    String curname = chronologyList.get(chronologyList.size() - 1);
                    int curid = (int) chronologyMap.get(curname);
                    chronologyMap.remove(curname);
                    chronologyList.remove(chronologyList.size() - 1);
                    chronologyStep--;
                    runPlayer(curid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        Button chronology = findViewById(R.id.playerLayout4Chronology);
        chronology.setVisibility(View.GONE);
    }
    public void initPlayerLayout4() {
        TextView playerLayout4dist = findViewById(R.id.playerLayout4dist);
        UpdatePlayerLayout4 updatePlayerLayout4 = new UpdatePlayerLayout4(playerLayout4dist);
        updatePlayerLayout4.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        Button exit = findViewById(R.id.playerLayout4Exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerLayout4.isRunning = false;
                MapPlayerFragment.isNew = false;
                setContentView(R.layout.quest_player);
                initPlayer();
            }
        });
        Button chronology = findViewById(R.id.playerLayout4Chronology);
        chronology.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerLayout4.isRunning = false;
                MapPlayerFragment.isNew = false;
                setContentView(R.layout.chronology_layout);
                initChronology();
            }
        });
    }

    private class UpdatePlayerLayout4 extends AsyncTask<Void, Integer, Void> {

        public boolean isRunning = true;
        long lastTime = 0;
        TextView textView;

        public UpdatePlayerLayout4(TextView textView) {
            this.textView = textView;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (isRunning) {
                if (System.currentTimeMillis() - lastTime > 500) {
                    lastTime = System.currentTimeMillis();
                    publishProgress(23);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            textView.setText("Расстояние до метки: " + ((int) MapPlayerFragment.distance) + "м");
            if((int) MapPlayerFragment.distance < 25) {
                MapPlayerFragment.isNew = false;
                isRunning = false;
                try {
                    runPlayer(MapPlayerFragment.idto);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initHelp(){
        Button back = findViewById(R.id.help_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.activity_main);
                initHome();
            }
        });
    }

    private void initHome(){

        boolean isPerm = false;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) isPerm = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                //todo when permission is granted
            } else {
                //request for the permission
                isPerm = false;
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }


        TextView textView = findViewById(R.id.textPermission);
        if(isPerm)
        textView.setVisibility(View.GONE);

        Button permission = findViewById(R.id.buttonPermission);
        if(isPerm)
            permission.setVisibility(View.GONE);
        permission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Check Permissions Now
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            1);
                }

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                                2);
                }
            }
        });

        File externalAppDir = new File(Environment.getExternalStorageDirectory() + "/CubesQuest");
        if (!externalAppDir.exists()) {
            Log.println(Log.ERROR, "LOG", Boolean.toString(externalAppDir.mkdir()));
            //System.out.println(externalAppDir.mkdir());
        }

        try {
            copy(getResources().openRawResource(R.raw.example_advanture), new File(Environment.getExternalStorageDirectory() + "/CubesQuest/Пример.quest"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Button play = findViewById(R.id.buttonPlay);
        play.setEnabled(isPerm);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.quest_player);
                initPlayer();
            }
        });

        Button edit = findViewById(R.id.buttonEdit);
        edit.setEnabled(isPerm);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.quest_redactor);
                initRedactor();
            }
        });

        Button help = findViewById(R.id.buttonHelp);
        help.setEnabled(isPerm);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.help_layout);
                initHelp();
            }
        });
    }

    public static void copy(InputStream src, File dst) throws IOException {
        try (InputStream in = src) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.println(Log.ERROR, "LOG", "GPS Permission Granted");
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                            2);
                }

            } else {
                //Toast.makeText(MainActivity.this, ">>> Mic Permission Denied", Toast.LENGTH_SHORT).show();
                Log.println(Log.ERROR, "LOG", "GPS Permission Denied");
            }
        }
        if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.println(Log.ERROR, "LOG", "File Permission Granted");

            } else {
                //Toast.makeText(MainActivity.this, ">>> Mic Permission Denied", Toast.LENGTH_SHORT).show();
                Log.println(Log.ERROR, "LOG", "File Permission Denied");
            }
        }

        initHome();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);;
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    2);
        }

        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // There are no request codes
                        Intent data = result.getData();
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = { MediaStore.Images.Media.DATA };
                        Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String picturePath = cursor.getString(columnIndex);
                        cursor.close();
                        ImageView imageView = new ImageView(MainActivity.this);
                        imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        imageView.setAdjustViewBounds(true);
                        Button deleteButton = new Button(MainActivity.this);
                        LinearLayout.LayoutParams params = new LinearLayout
                                .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        deleteButton.setLayoutParams(params);
                        deleteButton.setText("Удалить картинку выше");

                        editorChooseLayoutSlideEditorSlide.addView(imageView);
                        editorChooseLayoutSlideEditorSlide.addView(deleteButton);

                        deleteButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                editorChooseLayoutSlideEditorSlide.removeView(imageView);
                                editorChooseLayoutSlideEditorSlide.removeView(deleteButton);
                            }
                        });


                    }
                });


        questName = "";
        questStartId = "";

        initHome();


    }

    int backPressedQ = 0;

    @Override
    public void onBackPressed()
    {

        if (this.backPressedQ == 1)
        {
            this.backPressedQ = 0;
            super.onBackPressed();
        }
        else
        {
            this.backPressedQ++;
            Toast.makeText(this, "Нажмите ещё раз, чтобы выйти", Toast.LENGTH_SHORT).show();
        }
        //Обнуление счётчика через 5 секунд
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                // Do something after 5s = 5000ms
                backPressedQ = 0;
                //checkNew();
            }
        }, 3000);
    }

    public static double getRandomIntegerBetweenRange(double min, double max){
        return (Math.random()*((max-min)+1))+min;
    }


}