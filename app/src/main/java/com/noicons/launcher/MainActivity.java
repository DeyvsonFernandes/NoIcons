package com.noicons.launcher;

import android.annotation.SuppressLint;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ---------- Campos ----------

    private PackageManager pm;
    private RecyclerView lista;
    private AppAdapter adapter;

    private AlertDialog dialogPermissao;
    private ActivityResultLauncher<Intent> pedidoHome;
    private boolean jaPediuHome = false;

    private final BroadcastReceiver pacotesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            adapter.setApps(carregarApps());
        }
    };

    // ---------- Ciclo de vida ----------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pm = getPackageManager();

        configurarLista();
        registrarReceiver();
        configurarBotaoVoltar();

        pedidoHome = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> adapter.setApps(carregarApps()));
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        NotificationStore.setListener(() -> adapter.notifyDataSetChanged());
        adapter.setApps(carregarApps());
        verificarPermissaoNotificacoes();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NotificationStore.setListener(null);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        lista.scrollToPosition(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialogPermissao != null) dialogPermissao.dismiss();
        unregisterReceiver(pacotesReceiver);
    }

    // ---------- Configuração inicial ----------

    private void configurarLista() {
        lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppAdapter(carregarApps(), pm);
        lista.setAdapter(adapter);
    }

    private void registrarReceiver() {
        IntentFilter filtro = new IntentFilter();
        filtro.addAction(Intent.ACTION_PACKAGE_ADDED);
        filtro.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filtro.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filtro.addDataScheme("package");

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(pacotesReceiver, filtro, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(pacotesReceiver, filtro);
        }
    }

    private void configurarBotaoVoltar() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // não faz nada: o launcher não fecha
            }
        });
    }

    // ---------- Lista de apps ----------

    private List<ResolveInfo> carregarApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
        apps.sort((a, b) -> a.loadLabel(pm).toString()
                .compareToIgnoreCase(b.loadLabel(pm).toString()));
        return apps;
    }

    // ---------- Permissão de notificações ----------

    private void verificarPermissaoNotificacoes() {
        boolean ativo = NotificationManagerCompat
                .getEnabledListenerPackages(this).contains(getPackageName());

        if (ativo) {
            if (dialogPermissao != null && dialogPermissao.isShowing()) {
                dialogPermissao.dismiss();
            }
            pedirLauncherPadrao();
            return;
        }

        if (dialogPermissao != null && dialogPermissao.isShowing()) return;

        dialogPermissao = new AlertDialog.Builder(this)
                .setTitle("Permitir notificações")
                .setMessage("Para mostrar o número de notificações novas ao lado de cada app, "
                        + "ative o acesso a notificações para este launcher.\n\n"
                        + "Se a opção estiver bloqueada: Configurações → Apps → este app → "
                        + "menu ⋮ → Permitir configurações restritas.")
                .setPositiveButton("Permitir", (d, w) -> startActivity(
                        new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
                .setNegativeButton("Agora não", null)
                .show();
    }

    // ---------- Launcher padrão ----------

    private boolean ehLauncherPadrao() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);

        ResolveInfo ri = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null
                && getPackageName().equals(ri.activityInfo.packageName);
    }

    private void pedirLauncherPadrao() {
        if (jaPediuHome || ehLauncherPadrao()) return;
        jaPediuHome = true;

        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)
                    && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                pedidoHome.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME));
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Tornar app padrão")
                    .setMessage("Para usar este launcher como tela inicial, escolha-o "
                            + "como app de início padrão.")
                    .setPositiveButton("Configurar", (d, w) -> startActivity(
                            new Intent(Settings.ACTION_HOME_SETTINGS)))
                    .setNegativeButton("Agora não", null)
                    .show();
        }
    }
}