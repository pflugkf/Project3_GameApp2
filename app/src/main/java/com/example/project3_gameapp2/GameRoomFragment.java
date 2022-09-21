package com.example.project3_gameapp2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.project3_gameapp2.databinding.FragmentGameRoomBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;

public class GameRoomFragment extends Fragment {
    private static final String TAG = "game room fragment";
    FragmentGameRoomBinding binding;
    GameRoomFragmentListener mListener;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final String ARG_GAME_ID = "ARG_GAME";
    private static final int FULL_HAND = 7;

    private String gameInstanceID;

    private void setupUI() {
        getActivity().setTitle("Game Room");
    }
    public GameRoomFragment() {
        // Required empty public constructor
    }

    public static GameRoomFragment newInstance(String gameID) {
        GameRoomFragment fragment = new GameRoomFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GAME_ID, gameID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            gameInstanceID = getArguments().getString(ARG_GAME_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGameRoomBinding.inflate(inflater, container, false);
        getActivity().findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.toolbar).findViewById(R.id.buttonLeaveGame).setVisibility(View.VISIBLE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(gameInstanceID, "unogamechannel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        return binding.getRoot();
    }

    View gameItemView;
    Game gameInstance;
    String winnerID, winnerName, turn;
    ArrayList<Card> playerHand;
    RecyclerView cardHandRecyclerView;
    LinearLayoutManager linearLayoutManager;
    GameRoomRecyclerViewAdapter adapter;
    Card currentCard;
    DocumentReference turnDocRef, cardDocRef, gameStatusDocRef;
    ListenerRegistration turnListener, cardListener, gameStatusListener, handListener;
    boolean gameStart = true;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        db.collection("games").document(gameInstanceID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    gameInstance = task.getResult().toObject(Game.class);
                    binding.textViewGameTitle.setText(gameInstance.getGameTitle());

                    //Set document references for queries
                    gameStatusDocRef = db.collection("games").document(gameInstanceID)
                            .collection("gameStatus").document("current");

                    turnDocRef = db.collection("games").document(gameInstanceID)
                            .collection("turn").document("current");

                    cardDocRef = db.collection("games").document(gameInstanceID)
                            .collection("topCard").document("current");

                    //game turn set
                    HashMap<String, String> data = new HashMap<>();
                    data.put("currentTurn", gameInstance.currentTurn);
                    turnDocRef.set(data).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "Initial turn set");
                        }
                    });

                    //game turn snapshot listener
                    turnListener = turnDocRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                            if(value != null){
                                String currentTurnUserID = value.getString("currentTurn");
                                turn = currentTurnUserID;

                                if(currentTurnUserID != null) {
                                    db.collection("users").document(currentTurnUserID)
                                            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        User user = task.getResult().toObject(User.class);
                                                        binding.textViewTurn.setText(user.getFirstName() + "'s Turn");
                                                    }
                                                }
                                            });
                                }
                            }
                        }
                    });

                    //discard pile set
                    if(gameInstance.topCard.value.equals("Draw 4")) {
                        Log.d(TAG, "top card is Draw 4, changing to valid starting card");
                        gameInstance.setTopCard(new Card("4", "Red", ""));
                        Log.d(TAG, "Top card should be Red 4");
                        Log.d(TAG, "Top card is now " + gameInstance.topCard.color + " " + gameInstance.topCard.value);
                    }
                    cardDocRef.set(gameInstance.topCard).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "Initial top card set to " + gameInstance.topCard.color + " " + gameInstance.topCard.value);
                            currentCard = gameInstance.topCard;
                        }
                    });


                    //discard pile top card snapshot listener
                    cardListener = cardDocRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                            if(value != null) {
                                Card topCard = value.toObject(Card.class);
                                currentCard = topCard;
                                if(topCard != null) {
                                    binding.currentCardValue.setText(topCard.getValue());
                                    binding.currentCardImage.setColorFilter(Color.parseColor(topCard.getColor()));
                                }

                            }
                        }
                    });

                    //game status set
                    HashMap<String, Object> gameStatus = new HashMap<>();
                    gameStatus.put("gameFinished", gameInstance.gameFinished);
                    gameStatus.put("winner", "");
                    gameStatus.put("winnerID", "");
                    gameStatusDocRef.set(gameStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });

                    //game status snapshot listener
                    gameStatusListener = gameStatusDocRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                            Log.d(TAG, "gameFinished value: " + value.getBoolean("gameFinished"));
                            if(value.getBoolean("gameFinished")) {
                                Log.d(TAG, "game finished, deleting game here");
                                gameStatusListener.remove();

                                mListener.goBackToLobby(gameInstanceID, gameInstance.player1, gameInstance.player2);
                            }
                        }
                    });
                    gameStart = false;
                }
            }
        });
        gameItemView = view.findViewById(android.R.id.content);

        playerHand = new ArrayList<>();
        getPlayerHands();
        dealCards(mAuth.getCurrentUser().getUid());
        cardHandRecyclerView = binding.playerHandRecyclerView;
        cardHandRecyclerView.setHasFixedSize(false);
        linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        cardHandRecyclerView.setLayoutManager(linearLayoutManager);
        adapter = new GameRoomRecyclerViewAdapter(playerHand);
        cardHandRecyclerView.setAdapter(adapter);


        //draw button
        binding.drawCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(turn.equals(mAuth.getCurrentUser().getUid())){
                    Card newCard = new Card();

                    if(newCard.getValue().equals(currentCard.value) || newCard.getColor().equals(currentCard.color)) {
                        playCard(newCard);
                    } else {
                        String collectionName = "hand-" + mAuth.getCurrentUser().getUid();
                        DocumentReference documentReference = db.collection("games").document(gameInstanceID)
                                .collection(collectionName).document();
                        newCard.setCardID(documentReference.getId());

                        documentReference.set(newCard).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getActivity(), "Drew card" + newCard.value + " " + newCard.color, Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Drew card" + newCard.value + " " + newCard.color);
                                    switchTurn();
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.waiting_turn_text, Toast.LENGTH_SHORT).show();
                }
            }
        });

        getActivity().findViewById(R.id.toolbar).findViewById(R.id.buttonLeaveGame).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "*****Implement leave game function*****");
            }
        });
    }

    public void playCard(Card newTopCard) {
        if(newTopCard.getValue().equals("Draw 4")) {
            String[] colorSet = {"Red", "Green", "Yellow", "Blue"};

            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            b.setTitle("Please choose a color")
                    .setItems(colorSet, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            switch (i){
                                case 0:
                                    cardDocRef.update("color", "Red").addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Log.d(TAG, "chose red");
                                        }
                                    });
                                    break;
                                case 1:
                                    cardDocRef.update("color", "Green").addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Log.d(TAG, "chose green");
                                        }
                                    });
                                    break;
                                case 2:
                                    cardDocRef.update("color", "Yellow").addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Log.d(TAG, "chose yellow");
                                        }
                                    });
                                    break;
                                case 3:
                                    cardDocRef.update("color", "Blue").addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Log.d(TAG, "chose blue");
                                        }
                                    });
                                    break;
                            }
                        }
                    });
            b.create().show();

        }

        cardDocRef.set(newTopCard).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                //Log.d("qq", "new top card successfully set in playcard");
                String toastText = "Played" + newTopCard.getColor() + " " + newTopCard.getValue();
                if(playerHand.size() != 0) {
                    Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
                }
                if(newTopCard.getValue().equals("Draw 4")) {
                    String player;
                    if(turn.equals(gameInstance.player1)) {
                        player = gameInstance.player2;
                    } else {
                        player = gameInstance.player1;
                    }

                    String collectionName = "hand-" + player;
                    for(int i = 0; i < 4; i++) {
                        DocumentReference documentReference = db.collection("games").document(gameInstanceID)
                                .collection(collectionName).document();
                        Card newCard = new Card();
                        newCard.setCardID(documentReference.getId());

                        documentReference.set(newCard).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    //Log.d(TAG, "card added to " + player + "'s hand");
                                } else {
                                    AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
                                    b.setTitle("Error dealing cards")
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
                } else if(!newTopCard.getValue().equals("Skip")) {
                    switchTurn();
                }
            }
        });
    }

    public void switchTurn(){
        String newTurn;
        if(turn.equals(gameInstance.player1)) {
            newTurn = gameInstance.player2;
        } else {
            newTurn = gameInstance.player1;
        }

        turnDocRef.update("currentTurn", newTurn).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "new turn successfully updated in switchTurn");
                }
            }
        });
    }

    public void dealCards(String player) {
        String collectionName = "hand-" + player;
        for(int i = 0; i < FULL_HAND; i++) {
            DocumentReference documentReference = db.collection("games").document(gameInstanceID)
                    .collection(collectionName).document();
            Card newCard = new Card();
            newCard.setCardID(documentReference.getId());

            documentReference.set(newCard).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        //Log.d(TAG, "added card: " + newCard.value + " " + newCard.color + " to " + player + "'s hand");
                    } else {
                        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
                        b.setTitle("Error dealing cards")
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
    }

    //Create each player hand as a collection of card documents and add snapshot listeners to both
    //Listen for changes to the player's hand, when the player has one card left, and when the player has won
    public void getPlayerHands() {
        String path = "hand-" + mAuth.getCurrentUser().getUid();
        handListener = db.collection("games").document(gameInstanceID)
                .collection(path)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        playerHand.clear();
                        //Log.d(TAG, "snapshot listener for called for " + path);

                        for(QueryDocumentSnapshot doc : value) {
                            Card c = doc.toObject(Card.class);
                            playerHand.add(c);
                        }
                        adapter.notifyDataSetChanged();

                        if(playerHand.size() == 1) {
                            //TODO: use push notification to declare uno
                            Log.d(TAG, "player " + path + " has uno, push notification sent");

                            /*if(gameStart == false) {
                                mListener.unoCalled();
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), gameInstanceID)
                                        .setSmallIcon(R.drawable.uno_card)
                                        .setContentTitle("UNO!")
                                        .setContentText("Player has called UNO!")
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                        .setAutoCancel(true);

                                NotificationManagerCompat managerCompat = NotificationManagerCompat.from(getContext());
                                if (mAuth.getCurrentUser().getUid().equals(gameInstance.player1)) {
                                    managerCompat.notify(1, builder.build());
                                } else {
                                    managerCompat.notify(2, builder.build());
                                }
                            }*/
                            /*if(gameItemView != null) {
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), gameInstanceID)
                                        .setSmallIcon(R.drawable.uno_card)
                                        .setContentTitle("UNO!")
                                        .setContentText("Player has called UNO!")
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                        .setAutoCancel(true);

                                NotificationManagerCompat managerCompat = NotificationManagerCompat.from(getContext());
                                if (mAuth.getCurrentUser().getUid().equals(gameInstance.player1)) {
                                    managerCompat.notify(1, builder.build());
                                } else {
                                    managerCompat.notify(2, builder.build());
                                }
                            }*/
                        }

                        if(playerHand.size() == 0) {
                            Log.d(TAG, "turn value is: " + turn);
                            winnerID = turn;
                            Log.d(TAG, "winnerID value is: " + winnerID);
                            db.collection("users").document(winnerID)
                                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                User winner = task.getResult().toObject(User.class);
                                                winnerName = winner.getFirstName();

                                                HashMap<String, Object> status = new HashMap<>();
                                                status.put("gameFinished", true);
                                                status.put("winner", winnerName);
                                                status.put("winnerID", winnerID);
                                                gameStatusDocRef.set(status).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        Log.d(TAG, "removing listeners");
                                                        handListener.remove();
                                                        cardListener.remove();
                                                        turnListener.remove();
                                                    }
                                                });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    class GameRoomRecyclerViewAdapter extends RecyclerView.Adapter<GameRoomRecyclerViewAdapter.GameRoomViewHolder> {
        ArrayList<Card> cardArrayList;

        public GameRoomRecyclerViewAdapter(ArrayList<Card> cards){
            this.cardArrayList = cards;
        }
        @NonNull
        @Override
        public GameRoomRecyclerViewAdapter.GameRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_line_item, parent, false);
            GameRoomViewHolder gameRoomViewHolder = new GameRoomViewHolder(view);
            return gameRoomViewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull GameRoomRecyclerViewAdapter.GameRoomViewHolder holder, int position) {
            if(cardArrayList.size() != 0) {
                Card card = cardArrayList.get(position);
                holder.cardID = card.getCardID();
                holder.cardValue.setText(cardArrayList.get(position).getValue());
                holder.cardImage.setColorFilter(Color.parseColor(cardArrayList.get(position).getColor()));
            }
        }

        @Override
        public int getItemCount() {
            return cardArrayList.size();
        }

        class GameRoomViewHolder extends RecyclerView.ViewHolder {
            ImageView cardImage;
            TextView cardValue;
            String cardID;

            public GameRoomViewHolder(@NonNull View itemView) {
                super(itemView);
                cardImage = itemView.findViewById(R.id.imageViewCardBack);
                cardValue = itemView.findViewById(R.id.textViewCardValue);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(turn.equals(mAuth.getCurrentUser().getUid())){
                            String path = "hand-" + mAuth.getCurrentUser().getUid();
                            DocumentReference documentReference = db.collection("games").document(gameInstance.gameID)
                                    .collection(path).document(cardID);

                            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        Card playedCard = task.getResult().toObject(Card.class);
                                        if(playedCard.getValue().equals(currentCard.getValue()) ||
                                                playedCard.getColor().equals(currentCard.getColor()) ||
                                                playedCard.getValue().equals("Draw 4")) {

                                            playCard(playedCard);

                                            documentReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Log.d(TAG, "card " + cardID + " deleted");
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {

                                                }
                                            });
                                        } else {
                                            Toast.makeText(getActivity(), R.string.invalid_card_text, Toast.LENGTH_SHORT).show();
                                        }

                                    }
                                }
                            });
                        } else {
                            Toast.makeText(getActivity(), R.string.waiting_turn_text, Toast.LENGTH_SHORT).show();
                        }


                    }
                });
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (GameRoomFragmentListener) context;
    }

    interface GameRoomFragmentListener {
        void unoCalled(String playerName);
        void goBackToLobby(String gameID, String p1, String p2);
    }
}