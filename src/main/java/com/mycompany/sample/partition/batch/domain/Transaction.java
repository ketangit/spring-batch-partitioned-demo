package com.mycompany.sample.partition.batch.domain;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.math.BigDecimal;
import java.util.Date;

@XmlRootElement(name="transaction")
public class Transaction {

	private String account;

	private Date timestamp;

	private BigDecimal amount;

	@XmlJavaTypeAdapter(JaxbDateSerializer.class)
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	@Override
	public String toString() {
		return "Transaction{" +
				"account='" + account + '\'' +
				", timestamp=" + timestamp +
				", amount=" + amount +
				'}';
	}
}
