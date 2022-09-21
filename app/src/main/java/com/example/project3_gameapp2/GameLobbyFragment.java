package com.example.project3_gameapp2;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GameLobbyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GameLobbyFragment extends Fragment {
    private static final String TAG = "game lobby fragment";
    private FirebaseAuth mAuth;
    FirebaseFirestore db;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public GameLobbyFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GameLobbyFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GameLobbyFragment newInstance(String param1, String param2) {
        GameLobbyFragment fragment = new GameLobbyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_lobby, container, false);
    }

    ArrayList<Game> games;
    RecyclerView gameList;
    LinearLayoutManager linearLayoutManager;
    GameLobbyRecyclerViewAdapter adapter;
    ListenerRegistration game;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.game_lobby_fragment);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        games = new ArrayList<>();
        gameList = view.findViewById(R.id.gameList);
        gameList.setHasFixedSize(false);
        linearLayoutManager = new LinearLayoutManager(getContext());
        gameList.setLayoutManager(linearLayoutManager);
        adapter = new GameLobbyRecyclerViewAdapter(games);
        gameList.setAdapter(adapter);

        db.collection("games").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                games.clear();

                for(QueryDocumentSnapshot doc : value) {
                    Game game = doc.toObject(Game.class);
                    game.setGameID(doc.getId());
                    games.add(game);
                }
                adapter.notifyDataSetChanged();
            }
        });

        view.findViewById(R.id.buttonMakeGame).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DocumentReference docRef = db.collection("games").document();
                HashMap<String, Object> newGame = new HashMap<>();
                newGame.put("gameTitle", user.getDisplayName() + "'s Game");
                newGame.put("gameID", docRef.getId());
                newGame.put("player1", user.getUid());
                newGame.put("player2", "");
                newGame.put("topCard", new Card());
                newGame.put("currentTurn", user.getUid());
                newGame.put("moves", new HashMap<String, Object>());
                newGame.put("gameFinished", false);

                docRef.set(newGame).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            Log.d(TAG, "game created");
                            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
                            b.setTitle(R.string.make_game_alert_txt).setMessage(R.string.awaiting_player2_text).setCancelable(false);
                            b.setNegativeButton(R.string.cancel_button_text, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    Toast.makeText(getActivity(), R.string.player_cancel_text, Toast.LENGTH_SHORT).show();
                                }
                            });
                            AlertDialog waitBox = b.create();
                            waitBox.show();
                            CountDownTimer cdt;
                            cdt = new CountDownTimer(30000, 1000) {

                                @Override
                                public void onTick(long l) {
                                    Log.d(TAG, "onTick: " + l/1000);
                                    b.setMessage("Waiting for players..." + l/1000);
                                }

                                @Override
                                public void onFinish() {

                                    db.collection("games").document(docRef.getId())
                                            .delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    waitBox.dismiss();
                                                    Toast.makeText(getActivity(), R.string.no_player2_joined_text, Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            };

                            cdt.start();

                            game = db.collection("games").document(docRef.getId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                                    if(value != null) {
                                        String test = value.getString("player2");
                                        Log.d(TAG, "player2: " + test);
                                        if(value.getString("player2") != null && !test.equals("")) {
                                            game.remove();
                                            cdt.cancel();
                                            waitBox.dismiss();
                                            DocumentReference dr = db.collection("games").document(docRef.getId());
                                            dr.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if(task.isSuccessful()) {
                                                        Game game = task.getResult().toObject(Game.class);
                                                        mListener.joinGame(game);
                                                    }

                                                }
                                            });
                                        }
                                    }

                                }
                            });
                        } else {
                            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
                            b.setTitle("Error creating game")
                                    .setMessage(task.getException().getMessage())
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    });
                            b.create().show();
                        }
                    }
                });
            }
        });
    }

    class GameLobbyRecyclerViewAdapter extends RecyclerView.Adapter<GameLobbyRecyclerViewAdapter.GameLobbyViewHolder> {
        ArrayList<Game> gamesArrayList;

        public GameLobbyRecyclerViewAdapter (ArrayList<Game> games) {
            this.gamesArrayList = games;
        }

        @NonNull
        @Override
        public GameLobbyRecyclerViewAdapter.GameLobbyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.game_line_item, parent, false);
            GameLobbyViewHolder gameLobbyViewHolder = new GameLobbyRecyclerViewAdapter.GameLobbyViewHolder(view);
            return gameLobbyViewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull GameLobbyRecyclerViewAdapter.GameLobbyViewHolder holder, int position) {
            if(gamesArrayList.size() != 0) {
                Game gameObj = gamesArrayList.get(position);
                holder.gameTitle.setText(gameObj.getGameTitle());
                holder.game = gameObj;
            }
        }

        @Override
        public int getItemCount() {
            return gamesArrayList.size();
        }

        class GameLobbyViewHolder extends RecyclerView.ViewHolder {
            TextView gameTitle;
            Game game;

            public GameLobbyViewHolder(@NonNull View itemView) {
                super(itemView);
                gameTitle = itemView.findViewById(R.id.gameTitle);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(mAuth.getCurrentUser().getUid().equals(game.getPlayer1())) {
                            mListener.joinGame(game);
                        }else if(mAuth.getCurrentUser().getUid() != game.getPlayer1() && game.player2.isEmpty()) {
                            DocumentReference docRef = db.collection("games").document(game.gameID);
                            docRef.update("player2", mAuth.getCurrentUser().getUid())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                mListener.joinGame(game);
                                            }
                                        }
                                    });
                        }

                    }
                });
            }

        }
    }

    GameLobbyFragmentListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (GameLobbyFragmentListener) context;
    }

    interface GameLobbyFragmentListener {
        void joinGame(Game game);
    }
}