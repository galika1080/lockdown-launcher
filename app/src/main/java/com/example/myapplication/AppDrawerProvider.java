package com.example.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.util.Pair;

import java.util.List;

public class AppDrawerProvider extends AsyncTask<Void, Void, String> {

    AppDrawer target;
    AppDrawer.CustomAdapter adapter;

    public AppDrawerProvider(AppDrawer target, AppDrawer.CustomAdapter adapter) {
        this.target = target;
        this.adapter = adapter;
    }

    @Override
    protected String doInBackground(Void... voids) {
        PackageManager pm = target.getContext().getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);

        for (ResolveInfo ri : apps) {
            adapter.apps.add(new Pair<>(ri.activityInfo.packageName, ri.activityInfo.loadIcon(pm)));
        }

        return "Success";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        updateStuff();
    }

    public void updateStuff() {
        adapter.notifyDataSetChanged();
    }
}
