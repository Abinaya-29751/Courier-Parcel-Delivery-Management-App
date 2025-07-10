package com.abi.project.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abi.project.R
import com.abi.project.database.DatabaseHelper
import com.abi.project.model.Courier
import com.abi.project.activities.TrackingActivity
import com.abi.project.utils.NotificationHelper
import android.Manifest

class UserDashboardActivity : ComponentActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var username: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoCouriers: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnTrack: Button
    private lateinit var courierAdapter: UserCouriersAdapter
    private lateinit var notificationHelper: NotificationHelper
    private var isSelectionMode = false
    private var selectedCourier: Courier? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        initializeViews()
        setupListeners()
        setupRecyclerView()
        loadCouriers()
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private lateinit var btnLogout: Button
    private lateinit var btnDeliveryPerson: Button

    private fun initializeViews() {
        databaseHelper = DatabaseHelper(this)
        notificationHelper = NotificationHelper(this)

        // Initialize views
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        recyclerView = findViewById(R.id.recyclerViewCouriers)
        tvNoCouriers = findViewById(R.id.tvNoCouriers)
        btnSelect = findViewById(R.id.btnSelect)
        btnTrack = findViewById(R.id.btnTrack)
        btnLogout = findViewById(R.id.btnLogout)
        btnDeliveryPerson = findViewById(R.id.btnDeliveryPerson)
        btnDeliveryPerson.isEnabled = false

        // Get logged in username
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        username = sharedPref.getString("loggedInUsername", "") ?: ""
        tvWelcome.text = "Welcome, $username"

        btnLogout.setOnClickListener {
            logoutUser()
        }
        btnDeliveryPerson.setOnClickListener {
            fetchDeliveryPersonDetails()
        }
    }

    private fun showDeliveryPersonDialog(name: String, contact: String) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Delivery Person Details")
        builder.setMessage("Name: $name\nContact: $contact")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun fetchDeliveryPersonDetails() {
        if (selectedCourier == null) {
            Toast.makeText(this, "Please select a courier first", Toast.LENGTH_SHORT).show()
            return
        }

        // Safely handle the nullable ID
        selectedCourier?.id?.let { courierId ->
            val deliveryPerson = databaseHelper.getDeliveryPersonForCourier(courierId)
            if (deliveryPerson != null) {
                showDeliveryPersonDialog(deliveryPerson.name, deliveryPerson.contact)
            } else {
                Toast.makeText(this, "No delivery person assigned to this courier.", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "Invalid courier ID", Toast.LENGTH_SHORT).show()
    }

    private fun logoutUser() {
        // Clear stored session data
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.clear() // Remove all saved preferences
        editor.apply()

        // Redirect to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Close UserDashboardActivity
    }

    private fun setupListeners() {
        btnSelect.setOnClickListener {
            isSelectionMode = !isSelectionMode
            btnSelect.text = if (isSelectionMode) "Cancel" else "Select"
            courierAdapter.toggleSelectionMode(isSelectionMode)
            btnTrack.isEnabled = false
            selectedCourier = null
            btnDeliveryPerson.isEnabled = false
        }

        btnTrack.setOnClickListener {
            selectedCourier?.let { courier ->
                courier.id?.let { courierId ->
                    val intent = Intent(this, TrackingActivity::class.java)
                    intent.putExtra("courierId", courierId)
                    startActivity(intent)
                } ?: Toast.makeText(this, "Invalid courier ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        courierAdapter = UserCouriersAdapter(emptyList(), object : CourierSelectionListener {
            override fun onCourierSelected(courier: Courier) {
                selectedCourier = courier
                btnTrack.isEnabled = true
                btnDeliveryPerson.isEnabled = true
            }

            override fun onCourierDeselected() {
                selectedCourier = null
                btnTrack.isEnabled = false
                btnDeliveryPerson.isEnabled = false
            }
        })
        recyclerView.adapter = courierAdapter
    }

    private fun loadCouriers() {
        val couriers = databaseHelper.getAllCouriersForUser(username)

        if (couriers.isEmpty()) {
            tvNoCouriers.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoCouriers.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            courierAdapter.updateCouriers(couriers)
        }

        // Check for status updates
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        for (courier in couriers) {
            courier.id?.let { courierId ->
                val lastKnownStatus = sharedPref.getString("lastStatus_$courierId", "")
                if (courier.status != lastKnownStatus) {
                    notificationHelper.sendStatusUpdateNotification(
                        username,
                        courier.courierNumber,
                        courier.status
                    )
                    editor.putString("lastStatus_$courierId", courier.status)
                }
            }
        }
        editor.apply()
    }

    interface CourierSelectionListener {
        fun onCourierSelected(courier: Courier)
        fun onCourierDeselected()
    }

    inner class UserCouriersAdapter(
        private var couriers: List<Courier>,
        private val selectionListener: CourierSelectionListener
    ) : RecyclerView.Adapter<UserCouriersAdapter.CourierViewHolder>() {

        private var showCheckboxes = false

        fun toggleSelectionMode(enable: Boolean) {
            showCheckboxes = enable
            notifyDataSetChanged()
        }

        fun updateCouriers(newCouriers: List<Courier>) {
            couriers = newCouriers
            notifyDataSetChanged()
        }

        inner class CourierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
            val tvCourierNumber: TextView = itemView.findViewById(R.id.tvCourierNumber)
            val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
            val tvPlace: TextView = itemView.findViewById(R.id.tvPlace)
            val tvDeliveryPerson: TextView = itemView.findViewById(R.id.tvDeliveryPerson)
            val tvUserUsername: TextView = itemView.findViewById(R.id.tvUserUsername)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourierViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_courier, parent, false)
            return CourierViewHolder(view)
        }

        override fun onBindViewHolder(holder: CourierViewHolder, position: Int) {
            val courier = couriers[position]

            // Show or hide the checkbox
            holder.checkBox.visibility = if (showCheckboxes) View.VISIBLE else View.GONE
            holder.checkBox.isChecked = courier == selectedCourier

            holder.tvCourierNumber.text = "Courier #: ${courier.courierNumber}"
            holder.tvStatus.text = "Status: ${courier.status}"
            holder.tvPlace.text = "Location: ${courier.place}"
            holder.tvUserUsername.text = "User: ${courier.userUsername}"

            if (!courier.deliveryPersonName.isNullOrEmpty()) {
                holder.tvDeliveryPerson.visibility = View.VISIBLE
                holder.tvDeliveryPerson.text = "Delivery Person: ${courier.deliveryPersonName}"
            } else {
                holder.tvDeliveryPerson.visibility = View.GONE
            }

            // Handle checkbox selection
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectionListener.onCourierSelected(courier)
                } else {
                    selectionListener.onCourierDeselected()
                }
            }
        }

        override fun getItemCount() = couriers.size
    }
}