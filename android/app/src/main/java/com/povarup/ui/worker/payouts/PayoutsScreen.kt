package com.povarup.ui.worker.payouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.domain.Payout

@Composable
fun PayoutsScreen(payouts: List<Payout>) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Summary: ${payouts.size} payouts")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(payouts) { payout ->
                Text("${payout.id}: ${payout.rawStatus} - $${"%.2f".format(payout.amountCents / 100.0)}")
            }
        }
    }
}
