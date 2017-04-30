package icue.com.weeklyhours;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout layout1;
    private LinearLayout layout2;
    private Button submitButton;
    private NumberPicker np1;
    private NumberPicker np2;
    private LineChart chart;

    private class UserHour{
        private List<Integer> hours = new ArrayList<>();
        public UserHour() {
            for(int i=0; i<15; i++)
                hours.add(0);
        }
        public UserHour(String s){
            String[] parts = s.split(":");
            for(String ss : parts){
                hours.add(Integer.valueOf(ss));
            }
        }
        public int get(int i){
            return hours.get(i);
        }
        public void set(int i, int t){
            hours.set(i, t);
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for(int t:hours){
                sb.append(t).append(":");
            }
            sb.setLength(sb.length()-1);
            return sb.toString();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    layout2.setVisibility(LinearLayout.GONE);
                    layout1.setVisibility(LinearLayout.VISIBLE);
                    return true;
                case R.id.navigation_dashboard:
                    layout1.setVisibility(LinearLayout.GONE);
                    layout2.setVisibility(LinearLayout.VISIBLE);
                    getTable();
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout1 = (LinearLayout) findViewById(R.id.layout1);
        layout2 = (LinearLayout) findViewById(R.id.layout2);
        layout2.setVisibility(LinearLayout.GONE);
        submitButton = (Button) findViewById(R.id.buttonSubmit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { doSubmit();}
        });

        chart = (LineChart) findViewById(R.id.chart);

        np1 = (NumberPicker) findViewById(R.id.numberPicker1);
        String[] nums = new String[15];
        for(int i=0; i<nums.length; i++)
            nums[i] = Integer.toString(i);
        np1.setMinValue(1);
        np1.setMaxValue(15);
        np1.setWrapSelectorWheel(false);
        np1.setDisplayedValues(nums);
        np1.setValue(9);

        np2 = (NumberPicker) findViewById(R.id.numberPicker2);
        nums = new String[40];
        for(int i=0; i<nums.length; i++)
            nums[i] = Integer.toString(i);
        np2.setMinValue(1);
        np2.setMaxValue(40);
        np2.setWrapSelectorWheel(false);
        np2.setDisplayedValues(nums);
        np2.setValue(21);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private void showMsgBox(String title, String msg){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void doSubmit() {
        EditText et = (EditText) findViewById(R.id.editName);
        String name = et.getText().toString();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();

        ref.child(name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                EditText et = (EditText) findViewById(R.id.editName);
                String name = et.getText().toString();
                if(name.equals("")){
                    showMsgBox("Failure","Please enter your name.");
                    return;
                }
                int week = np1.getValue();
                int hours = np2.getValue();
                UserHour res;
                if(dataSnapshot.exists()){
                    String temp = (String) dataSnapshot.getValue();
                    res = new UserHour(temp);
                    res.set(week-1, hours-1);
                } else {
                    res = new UserHour();
                    res.set(week-1, hours-1);
                }
                FirebaseDatabase.getInstance().getReference().child(name)
                        .setValue(res.toString(), new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    showMsgBox("Failure", "Data could not be saved " + databaseError.getMessage());
                                } else {
//                                    showMsgBox("Success", "Your hours has been logged successfully.");
                                    Toast.makeText(getBaseContext(), "Your hours has been logged successfully.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getTable() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    int[] sum = new int[15];
                    int[] size = new int[15];
                    TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
                    tableLayout.removeAllViews();
                    TableRow tableRow;
                    TextView textView;

                    for (DataSnapshot child: dataSnapshot.getChildren()) {
                        final String key = child.getKey();
                        String value = (String) child.getValue();
                        final UserHour res = new UserHour(value);

                        tableRow = new TableRow(getApplicationContext());
                        textView = new TextView(getApplicationContext());
                        textView.setTextColor(Color.rgb(0,0,0));
                        textView.setText(key);
                        textView.setPadding(20, 20, 20, 20);

                        textView.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                List<Entry> entries = new ArrayList<>();
                                for(int i=0;i<15;i++)
                                    entries.add(new Entry(i+1, res.get(i)));
                                updateChart(key,entries);
                            }
                        });

                        tableRow.addView(textView);
                        for(int i=0;i<15;i++) {
                            sum[i]+=res.get(i);
                            if(res.get(i)!=0) size[i]++;
                            textView = new TextView(getApplicationContext());
                            textView.setText(String.valueOf(res.get(i)));
                            textView.setPadding(10, 12, 10, 12);
                            textView.setTextColor(Color.rgb(0,0,0));
                            textView.setBackgroundColor(getMyColor(res.get(i)));
                            tableRow.addView(textView);
                        }
                        tableRow.setBackgroundResource(R.drawable.row_border);
                        tableLayout.addView(tableRow);
                    }

                    List<Entry> entries = new ArrayList<>();
                    for(int i=0;i<15;i++)
                        entries.add(new Entry(i+1, (float) sum[i]/size[i]));
                    updateChart("Average",entries);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private int getMyColor(int d) {
//        Red 255, 0, 0
//        Yellow 255, 255, 0
//        Green 0, 255, 0
        if(d==0) return Color.rgb(106,112,109);
        if(d<20) return Color.argb(180,d*12,255,0);
        if(d==20) return Color.argb(180,255,255,0);
        return Color.argb(180,255,255-(d-20)*12,0);
    }

    private void updateChart(String name, List<Entry> entries) {
        Description description = new Description();
        description.setText("");
        chart.setDescription(description);

        LineDataSet dataSet = new LineDataSet(entries, name); // add entries to dataset
        int color = ContextCompat.getColor(this, R.color.colorAccent);
        dataSet.setColor(color);
        dataSet.setLineWidth(5);
        dataSet.setValueTextSize(12f);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.setDragEnabled(true);
        chart.setScaleXEnabled(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.animateXY(1000, 1000);
    }
}
