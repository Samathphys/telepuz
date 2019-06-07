package com.example.sch;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toolbar;

import java.util.ArrayList;

public class TotalMarks extends Fragment {

    ArrayList<String>[] s;
    TableLayout tableLayout;
    Toolbar toolbar;

    public TotalMarks() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_total_marks, container, false);
        tableLayout = v.findViewById(R.id.table);
        s = ((MainActivity) getActivity()).scheduleFragment.s;

        for (int i = 0; i < s[0].size(); i++) {
            TableRow tableRow = new TableRow(getContext());
            for (int j = 0; j < s.length; j++) {
                TextView txt = new TextView(getContext());
                if (i == 0) {
                    txt.setText(s[j].get(i));
                    txt.setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.0f, getResources().getDisplayMetrics()), 0.4f);
                } else {
                    txt.setText(s[j].get(i));
                }
                txt.setGravity(Gravity.CENTER);
                tableRow.addView(txt);
            }
            tableLayout.addView(tableRow);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        }
        return v;
    }

    String ss(String s) {
        String f = "";
        for (int i = 0; i < s.length(); i++) {
            f += s.charAt(i);
            if (i != s.length() - 1)
                f += "\n";
        }
        return f;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }

}
