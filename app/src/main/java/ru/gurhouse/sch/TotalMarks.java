package ru.gurhouse.sch;

import android.content.Context;
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

/** under construction - screen showing total marks of the user*/
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
        if (((MainActivity) getActivity()).scheduleFragment.s != null && ((MainActivity) getActivity()).scheduleFragment.s[0].size() > 1) {
            s = ((MainActivity) getActivity()).scheduleFragment.s;
        } else {
            ((MainActivity) getActivity()).scheduleFragment.Download3();
        }

        for (int i = 0; i < s[0].size(); i++) {
            TableRow tableRow = new TableRow(getContext());
            for (ArrayList<String> strings : s) {
                TextView txt = new TextView(getContext());
                if (i == 0) {
                    txt.setText(strings.get(i));
                    txt.setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.0f, getResources().getDisplayMetrics()), 0.4f);
                } else {
                    txt.setText(strings.get(i));
                }
                txt.setGravity(Gravity.CENTER);
                tableRow.addView(txt);
            }
            tableLayout.addView(tableRow);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }

}
