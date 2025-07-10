package com.abi.project.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abi.project.model.Courier
import com.abi.project.database.DatabaseHelper
import com.abi.project.R
import com.abi.project.utils.NotificationHelper

class AdminDashboardActivity : ComponentActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var couriersAdapter: CouriersAdapter
    private var couriersList: MutableList<Courier> = mutableListOf()
    private lateinit var notificationHelper: NotificationHelper

    // UI Components
    private lateinit var addCourierForm: LinearLayout
    private lateinit var btnAddCourier: Button
    private lateinit var btnSave: Button
    private lateinit var etCourierNumber: EditText
    private lateinit var etUserUsername: EditText
    private lateinit var spinnerStatus: Spinner
    private lateinit var etPlace: EditText
    private lateinit var etDeliveryPersonName: EditText
    private lateinit var etDeliveryPersonId: EditText
    private lateinit var etCourierLocation: EditText
    private lateinit var rvCouriers: RecyclerView

    private var selectedCourierId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        initializeViews()
        setupSpinner()
        setupRecyclerView()
        setupListeners()
        loadCouriers() // Load initial data
    }

    private fun initializeViews() {
        // Initialize helpers
        databaseHelper = DatabaseHelper(this)
        notificationHelper = NotificationHelper(this)

        // Find views
        etCourierNumber = findViewById(R.id.etCourierNumber)
        etPlace = findViewById(R.id.etPlace)
        etUserUsername = findViewById(R.id.etUserUsername)
        etDeliveryPersonName = findViewById(R.id.etDeliveryPersonName)
        etDeliveryPersonId = findViewById(R.id.etDeliveryPersonId)
        etCourierLocation = findViewById(R.id.etCourierLocation)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        addCourierForm = findViewById(R.id.addCourierForm)
        btnAddCourier = findViewById(R.id.btnAddCourier)
        btnSave = findViewById(R.id.btnSave)
        rvCouriers = findViewById(R.id.rvCouriers)

        // Set initial form visibility
        addCourierForm.visibility = View.GONE

        // Setup logout button
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            handleLogout()
        }
    }

    private fun handleLogout() {
        // Clear shared preferences
        getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // Navigate to login
        Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
        finish()
    }

    private fun setupSpinner() {
        val statusOptions = arrayOf("Picked up", "Out to deliver", "In transit", "Delivered")
        ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStatus.adapter = this
        }
    }

    private fun setupRecyclerView() {
        rvCouriers.layoutManager = LinearLayoutManager(this)
        couriersAdapter = CouriersAdapter(couriersList) { courier ->
            handleCourierSelection(courier)
        }
        rvCouriers.adapter = couriersAdapter
    }

    private fun handleCourierSelection(courier: Courier) {
        selectedCourierId = courier.id
        etCourierNumber.setText(courier.courierNumber)
        etUserUsername.setText(courier.userUsername)
        etPlace.setText(courier.place)
        etDeliveryPersonName.setText(courier.deliveryPersonName ?: "")
        etDeliveryPersonId.setText(courier.deliveryPersonId ?: "")
        etCourierLocation.setText(courier.locationUrl ?: "")

        // Set spinner selection
        val statusOptions = arrayOf("Picked up", "Out to deliver", "In transit", "Delivered")
        statusOptions.indexOf(courier.status).takeIf { it != -1 }?.let {
            spinnerStatus.setSelection(it)
        }

        addCourierForm.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        btnAddCourier.setOnClickListener {
            if (addCourierForm.visibility == View.GONE) {
                clearForm()
                addCourierForm.visibility = View.VISIBLE
                selectedCourierId = null
            } else {
                addCourierForm.visibility = View.GONE
            }
        }

        btnSave.setOnClickListener {
            handleSaveCourier()
        }
    }

    private fun handleSaveCourier() {
        val courierNumber = etCourierNumber.text.toString().trim()
        val username = etUserUsername.text.toString().trim()
        val status = spinnerStatus.selectedItem.toString()
        val place = etPlace.text.toString().trim()
        val deliveryPerson = etDeliveryPersonName.text.toString().trim()
        val deliveryPersonId = etDeliveryPersonId.text.toString().trim()
        val locationUrl = etCourierLocation.text.toString().trim()

        if (courierNumber.isEmpty() || username.isEmpty() || locationUrl.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val success = if (selectedCourierId == null) {
            // Insert new courier
            val id = databaseHelper.insertCourier(
                courierNumber, username, status, place,
                deliveryPerson, deliveryPersonId, locationUrl
            )
            if (id != -1L) {
                Toast.makeText(this, "Courier Added Successfully", Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(this, "Failed to add courier", Toast.LENGTH_SHORT).show()
                false
            }
        } else {
            // Update existing courier
            val updated = databaseHelper.updateCourierStatus(selectedCourierId!!, status)
            if (updated) {
                Toast.makeText(this, "Courier status updated successfully", Toast.LENGTH_SHORT).show()
                notificationHelper.sendStatusUpdateNotification(username, courierNumber, status)
                true
            } else {
                Toast.makeText(this, "Failed to update courier status", Toast.LENGTH_SHORT).show()
                false
            }
        }

        if (success) {
            clearForm()
            loadCouriers()
            addCourierForm.visibility = View.GONE
        }
    }

    private fun loadCouriers() {
        couriersList.clear()
        couriersList.addAll(databaseHelper.getAllCouriers())
        couriersAdapter.notifyDataSetChanged()
    }

    private fun clearForm() {
        etCourierNumber.text.clear()
        etUserUsername.text.clear()
        etPlace.text.clear()
        etDeliveryPersonName.text.clear()
        etDeliveryPersonId.text.clear()
        etCourierLocation.text.clear()
        spinnerStatus.setSelection(0)
    }

    inner class CouriersAdapter(
        private var couriers: List<Courier>,
        private val onItemClick: (Courier) -> Unit
    ) : RecyclerView.Adapter<CouriersAdapter.CourierViewHolder>() {

        inner class CourierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvCourierNumber: TextView = itemView.findViewById(R.id.tvCourierNumber)
            val tvUserUsername: TextView = itemView.findViewById(R.id.tvUserUsername)
            val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
            val tvPlace: TextView = itemView.findViewById(R.id.tvPlace)
            val tvDeliveryPerson: TextView = itemView.findViewById(R.id.tvDeliveryPerson)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourierViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_courier, parent, false)
            return CourierViewHolder(view)
        }

        override fun onBindViewHolder(holder: CourierViewHolder, position: Int) {
            val courier = couriers[position]
            holder.tvCourierNumber.text = "Courier #: ${courier.courierNumber}"
            holder.tvUserUsername.text = "User: ${courier.userUsername}"
            holder.tvStatus.text = "Status: ${courier.status}"
            holder.tvPlace.text = "Place: ${courier.place ?: "N/A"}"
            holder.tvDeliveryPerson.text = "Delivery Person: ${courier.deliveryPersonName ?: "N/A"}"

            holder.itemView.setOnClickListener { onItemClick(courier) }
        }

        override fun getItemCount() = couriers.size
    }
}