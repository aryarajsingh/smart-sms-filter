#!/usr/bin/env python3
"""
Enhanced Synthetic Training Script for Mobile SMS Classifier
Expanded data generation with more diverse patterns and edge cases
"""

import os
import sys
import logging
import pandas as pd
import numpy as np
from pathlib import Path
from sklearn.model_selection import train_test_split
from sklearn.utils.class_weight import compute_class_weight
import random

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def create_enhanced_synthetic_data():
    """Create enhanced synthetic training data with more diverse patterns"""
    
    logger.info("Creating enhanced synthetic training data...")
    
    # SMS categories
    categories = {
        'INBOX': 0, 'SPAM': 1, 'OTP': 2, 
        'BANKING': 3, 'ECOMMERCE': 4, 'NEEDS_REVIEW': 5
    }
    
    training_data = []
    samples_per_category = 200  # Increased from 150 to 200
    
    # ============= EXPANDED OTP MESSAGES =============
    logger.info("Generating OTP messages...")
    
    # More diverse OTP templates
    otp_templates = [
        # Standard formats
        "{code} is your OTP for {service}. Valid for {time} minutes. Do not share.",
        "Your verification code is {code}. Valid for {time} minutes only.",
        "OTP: {code} for {service} login. Do not share with anyone.",
        "Use {code} to verify your account. One Time Password valid for {time} minutes.",
        "{code} is your security code for {service}. Don't share with anyone.",
        "Dear Customer, {code} is your OTP for {service}. Valid till {time} mins.",
        "Your OTP is {code} for {service}. Valid for {time} minutes only.",
        "Verification code: {code}. Valid for {time} minutes. Do not share.",
        "{code} is your authentication code for {service}. Expires in {time} minutes.",
        "Your login OTP is {code}. Valid for {time} minutes. Keep it confidential.",
        "Code {code} to complete your registration. Valid for {time} mins.",
        "Authentication code: {code}. Expires in {time} minutes.",
        "Confirm with {code}. This code is valid for {time} minutes.",
        "LOGIN CODE: {code}. Do not share with anyone.",
        "Security code {code} for account verification. Expires soon.",
        
        # Modern app formats
        "{service}: Your verification code is {code}",
        "Hi! {code} is your {service} verification code",
        "{code} is your {service} security code",
        "Your {service} login code: {code}",
        "{service} verification: {code}",
        
        # Short formats
        "OTP {code}",
        "Code: {code}",
        "{code} - {service}",
        "Verify: {code}",
        
        # Urgent/Security focused
        "URGENT: Use {code} to secure your {service} account",
        "Security Alert: {code} is your verification code for {service}",
        "Account Security: Your code is {code}",
        
        # Multi-language hints
        "Your OTP hai {code}. Please verify karo.",
        "OTP {code} use karke login karo {service} mein",
    ]
    
    services = [
        "HDFC Bank", "SBI", "ICICI Bank", "Axis Bank", "Kotak Bank", "Yes Bank",
        "PayTM", "PhonePe", "GPay", "Amazon Pay", "Mobikwik", "FreeCharge",
        "WhatsApp", "Instagram", "Facebook", "Twitter", "LinkedIn", "Snapchat",
        "Amazon", "Flipkart", "Myntra", "Swiggy", "Zomato", "BigBasket",
        "Uber", "Ola", "Make My Trip", "BookMyShow", "Hotstar", "Netflix",
        "Airtel", "Jio", "Vi", "BSNL", "Tata Sky", "Dish TV",
        "Google", "Microsoft", "Apple", "Samsung", "OnePlus", "Mi"
    ]
    
    for i in range(samples_per_category):
        template = random.choice(otp_templates)
        code = random.choice([
            str(random.randint(1000, 9999)),        # 4-digit
            str(random.randint(10000, 99999)),      # 5-digit
            str(random.randint(100000, 999999)),    # 6-digit
            str(random.randint(1000000, 9999999))   # 7-digit
        ])
        service = random.choice(services)
        time = random.choice([1, 2, 3, 5, 10, 15, 30])
        
        try:
            msg = template.format(code=code, service=service, time=time)
            training_data.append((msg, categories['OTP']))
        except:
            # Simple fallback
            msg = f"{code} is your OTP for {service}. Valid for {time} minutes."
            training_data.append((msg, categories['OTP']))
    
    # ============= EXPANDED BANKING MESSAGES =============
    logger.info("Generating Banking messages...")
    
    banking_templates = [
        # Transaction alerts
        "Rs.{amount} debited from A/c XX{acc} on {date} by {mode}. Balance: Rs.{balance}",
        "Rs.{amount} credited to your account XX{acc} from {name}. Balance Rs.{balance}",
        "Dear Customer, Rs.{amount} has been debited from A/c {acc} for {purpose}. Balance Rs.{balance}",
        "Rs.{amount} transferred from {bank1} A/c XX{acc} to {name}. Ref: {ref}",
        "Your {bank} account XX{acc} credited with Rs.{amount} from {purpose}. Balance Rs.{balance}",
        "ATM withdrawal of Rs.{amount} from {bank} ATM. A/c XX{acc} Balance Rs.{balance}",
        "Card payment of Rs.{amount} at {merchant}. Transaction successful. Balance: Rs.{balance}",
        "UPI payment of Rs.{amount} to {merchant} successful via {app}. Balance: Rs.{balance}",
        "NEFT transfer of Rs.{amount} initiated successfully. Reference: {ref}",
        "IMPS of Rs.{amount} received from {name}. A/c XX{acc} Balance: Rs.{balance}",
        
        # Bills and payments
        "Auto-debit of Rs.{amount} for {purpose} successful from {bank} A/c XX{acc}",
        "Your {utility} bill of Rs.{amount} has been paid successfully. Balance: Rs.{balance}",
        "EMI of Rs.{amount} debited from A/c XX{acc} for {loan_type}. Balance Rs.{balance}",
        "SIP of Rs.{amount} debited for {fund} investment. A/c XX{acc} Balance Rs.{balance}",
        "Mobile recharge of Rs.{amount} successful for {phone}. Balance: Rs.{balance}",
        "DTH recharge of Rs.{amount} for {service} successful. A/c XX{acc}",
        
        # Credit cards
        "Your {bank} credit card bill of Rs.{amount} is due on {date}. Pay to avoid charges.",
        "Minimum amount due Rs.{amount} for {bank} Credit Card. Pay by {date}",
        "Credit card payment of Rs.{amount} received. Outstanding: Rs.{balance}",
        "Your {bank} credit card used for Rs.{amount} at {merchant}",
        
        # Investments
        "Fixed deposit of Rs.{amount} matured. Interest Rs.{interest} credited to A/c XX{acc}",
        "Your mutual fund investment of Rs.{amount} completed successfully",
        "Dividend of Rs.{amount} credited to your account for {fund}",
        
        # Account services  
        "Cheque no. {cheque} for Rs.{amount} cleared. A/c XX{acc} Balance Rs.{balance}",
        "Account statement for {bank} A/c XX{acc} generated. Download from mobile app",
        "Your {bank} debit card ending {card} has been blocked for security",
        "New {bank} credit card approved. Limit: Rs.{amount}. Card will arrive in 7 days",
        
        # Modern banking
        "UPI AutoPay mandate of Rs.{amount} for {service} activated successfully",
        "Your {bank} account is now linked with UPI ID {name}@{app}",
        "Instant loan of Rs.{amount} approved. Amount credited to A/c XX{acc}",
        "{bank} FD opened for Rs.{amount} at {rate}% interest for {tenure} months",
        
        # Declined/Failed
        "Transaction of Rs.{amount} declined due to insufficient balance",
        "UPI payment failed. Please try again or contact bank",
        "Card blocked due to suspicious activity. Contact {bank} immediately",
        
        # Alerts and notifications
        "Low balance alert: Your {bank} A/c XX{acc} balance is Rs.{balance}",
        "Salary of Rs.{amount} credited to your {bank} account. Balance Rs.{balance}",
        "Your {bank} account will be debited Rs.{amount} for {purpose} tomorrow",
    ]
    
    banks = ["HDFC", "SBI", "ICICI", "Axis", "Kotak", "Yes Bank", "PNB", "BOB", "Canara", "Union Bank"]
    merchants = ["Amazon", "Flipkart", "Swiggy", "Zomato", "BigBasket", "DMart", "Reliance", "Metro", "Spencer's"]
    names = ["JOHN", "RAVI", "PRIYA", "AMIT", "NEHA", "ROHIT", "ANJALI", "VIKASH", "POOJA", "RAJESH"]
    utilities = ["electricity", "gas", "water", "mobile", "broadband", "DTH", "insurance", "loan EMI"]
    apps = ["PayTM", "PhonePe", "GPay", "BHIM", "Amazon Pay"]
    loan_types = ["home loan", "car loan", "personal loan", "education loan"]
    funds = ["HDFC Equity Fund", "SBI MF", "ICICI Bluechip", "Axis Midcap"]
    modes = ["UPI", "NetBanking", "Card", "IMPS", "NEFT", "RTGS"]
    
    for i in range(samples_per_category):
        template = random.choice(banking_templates)
        data = {
            'amount': random.choice([
                random.randint(100, 1000),      # Small amounts
                random.randint(1000, 10000),    # Medium amounts  
                random.randint(10000, 100000),  # Large amounts
                random.randint(100000, 1000000) # Very large amounts
            ]),
            'acc': random.randint(1000, 9999),
            'date': f"{random.randint(1,28)}-{random.choice(['JAN','FEB','MAR','APR','MAY','JUN'])}-25",
            'phone': f"98{random.randint(10000000, 99999999)}",
            'balance': random.randint(5000, 500000),
            'name': random.choice(names),
            'purpose': random.choice(utilities + ["shopping", "fuel", "medicine", "salary", "refund"]),
            'merchant': random.choice(merchants),
            'ref': f"UPI{random.randint(100000000, 999999999)}",
            'interest': random.randint(100, 5000),
            'cheque': random.randint(100000, 999999),
            'utility': random.choice(utilities),
            'bank': random.choice(banks),
            'bank1': random.choice(banks),
            'mode': random.choice(modes),
            'app': random.choice(apps),
            'loan_type': random.choice(loan_types),
            'fund': random.choice(funds),
            'service': random.choice(["Netflix", "Amazon Prime", "Hotstar", "Spotify", "Jio"]),
            'card': random.randint(1000, 9999),
            'rate': random.choice([6.5, 7.0, 7.5, 8.0, 8.5, 9.0]),
            'tenure': random.choice([12, 24, 36, 60])
        }
        
        try:
            msg = template.format(**data)
            training_data.append((msg, categories['BANKING']))
        except:
            continue
    
    # ============= EXPANDED SPAM MESSAGES =============
    logger.info("Generating SPAM messages...")
    
    spam_templates = [
        # Prize/lottery scams
        "🎉 CONGRATULATIONS! You've won Rs.{prize}! Call {phone} to claim NOW!",
        "WINNER WINNER! You're selected for {item} worth Rs.{prize}! Call {phone}",
        "URGENT: You've won {item}! Click {link} to claim within 24 hours!",
        "Lottery winner announced! You won Rs.{prize}. SMS WIN to {shortcode}",
        "🎁 You're today's lucky winner! Get {item} FREE! Call {phone} now!",
        
        # Fake offers
        "LIMITED TIME! {discount}% OFF on everything! Shop now at {link}",
        "FLASH SALE: Get {discount}% discount on {category}. Buy now!",
        "AMAZING OFFER! Buy 1 Get {num} FREE! Limited stock. Order now!",
        "🔥 HOTTEST DEALS! {category} starting at Rs.{price}! Don't miss!",
        "MEGA SALE: Up to {discount}% off on {category}. Hurry!",
        
        # Fake urgency
        "URGENT ALERT: Your account will be suspended. Click {link} immediately",
        "FINAL NOTICE: Pay Rs.{amount} to avoid service disconnection",
        "Your {service} expires today! Renew now at {link}",
        "IMMEDIATE ACTION REQUIRED: Update your KYC to avoid account block",
        "Security breach detected! Verify at {link} within 2 hours",
        
        # Loan scams
        "GUARANTEED LOAN! Get Rs.{amount} without documents. Call {phone}",
        "Instant cash! Rs.{amount} approved. No verification needed",
        "Pre-approved loan of Rs.{amount}. Call {phone} for instant disbursal",
        "Easy loan! Rs.{amount} in 15 minutes. Apply now!",
        "Personal loan @ {rate}% interest. Get Rs.{amount} today!",
        
        # Job/earning scams
        "Make Rs.{amount} from home! No investment required. Call {phone}",
        "Part time job! Earn Rs.{amount} daily. SMS JOIN to {shortcode}",
        "Work from home opportunity! Rs.{amount} per month guaranteed",
        "Copy paste job! Earn Rs.{amount} weekly. No experience needed",
        
        # Investment scams
        "Double your money in {days} days! Invest Rs.{amount} get Rs.{prize}",
        "Crypto investment opportunity! Turn Rs.{amount} into Rs.{prize}",
        "Stock tip: Buy {stock}. Guaranteed {percent}% profit!",
        "Binary trading! Make Rs.{amount} daily. Join now!",
        
        # Fake services
        "CHEAP medicines online! {discount}% off. No prescription needed",
        "Free iPhone! Just pay delivery Rs.{amount}. Order now!",
        "Unlock any phone for Rs.{amount}. 100% working method",
        "Free recharge trick! Get Rs.{amount} talktime. Try now!",
        
        # Modern scams
        "OTP verification failed. Click {link} to complete KYC",
        "Your Aadhaar is suspended. Update immediately at {link}",
        "PAN card verification pending. Complete at {link}",
        "GST registration required. Apply now for Rs.{amount}",
        
        # Fake threats
        "Legal notice: Pay Rs.{amount} fine or face arrest",
        "Income tax notice: Clear dues of Rs.{amount} immediately",
        "Your SIM will be blocked. Call {phone} to resolve",
        "Bank account frozen! Unblock by paying Rs.{amount}",
    ]
    
    items = ["iPhone 15", "Laptop", "Car", "Gold", "Diamond", "TV", "AC", "Bike", "Watch", "Tablet"]
    categories_spam = ["mobiles", "electronics", "fashion", "medicines", "books", "grocery", "furniture"]
    services = ["broadband", "mobile", "DTH", "electricity", "gas", "insurance"]
    stocks = ["RELIANCE", "TCS", "INFY", "HDFC", "ICICI", "SBI", "ITC", "WIPRO"]
    
    for i in range(samples_per_category):
        template = random.choice(spam_templates)
        data = {
            'prize': random.randint(10000, 5000000),
            'phone': f"98{random.randint(10000000, 99999999)}",
            'item': random.choice(items),
            'link': f"http://{random.choice(['bit.ly', 'tinyurl.com', 'short.ly'])}/{random.choice(['offer', 'claim', 'win', 'get', 'free'])}{random.randint(100,999)}",
            'discount': random.randint(30, 90),
            'num': random.randint(1, 5),
            'shortcode': random.randint(56789, 99999),
            'category': random.choice(categories_spam),
            'price': random.randint(99, 9999),
            'amount': random.randint(500, 100000),
            'service': random.choice(services),
            'rate': random.choice([8, 9, 10, 12, 15]),
            'days': random.choice([7, 15, 30, 60, 90]),
            'percent': random.randint(50, 500),
            'stock': random.choice(stocks)
        }
        
        try:
            msg = template.format(**data)
            training_data.append((msg, categories['SPAM']))
        except:
            continue
    
    # ============= EXPANDED ECOMMERCE MESSAGES =============
    logger.info("Generating E-commerce messages...")
    
    ecommerce_templates = [
        # Order updates
        "Your {platform} order #{order_id} is {status}. Expected by {time}",
        "Order #{order_id} {status} successfully. Rate your experience on {platform}",
        "Your {platform} order #{order_id} is shipped. Track: {link}",
        "Payment of Rs.{amount} for order #{order_id} confirmed. Thank you!",
        "{platform}: Your order worth Rs.{amount} is being prepared for delivery",
        "Order #{order_id} is delayed. New delivery: {date}. Sorry for inconvenience",
        
        # Delivery updates  
        "{platform} delivery update: Your order will arrive in {time} minutes",
        "Your order from {restaurant} is ready for pickup",
        "Delivery partner assigned for order #{order_id}. Track live",
        "Your order has reached the delivery hub. Arriving soon!",
        "Failed delivery attempt for #{order_id}. Reschedule delivery?",
        
        # Ride services
        "Your {platform} ride receipt: Rs.{amount} for trip on {date}",
        "Trip completed! Rs.{amount} charged to your wallet",
        "Your {platform} driver will arrive in {time} minutes",
        "Ride cancelled. Rs.{amount} refunded to original payment method",
        "Trip fare: Rs.{amount}. Rate your driver on {platform}",
        
        # Wallet and payments
        "{platform}: Rs.{amount} added to wallet successfully",
        "Cashback of Rs.{amount} credited for order #{order_id}",
        "Use code {code} for {discount}% cashback on next order",
        "Wallet balance low. Add money to continue using {platform}",
        "Payment failed for order #{order_id}. Try different payment method",
        
        # Returns and refunds
        "Return request approved for #{order_id}. Refund Rs.{amount} initiated",
        "Your refund of Rs.{amount} has been processed to your account",
        "Replacement for order #{order_id} will be delivered by {date}",
        "Return pickup scheduled for #{order_id} on {date}",
        
        # Subscriptions
        "Your {platform} subscription renewed. Next billing: Rs.{amount} on {date}",
        "{platform} Plus membership activated. Enjoy free delivery!",
        "Subscription expires in {days} days. Renew to continue benefits",
        "Auto-renewal failed for {platform} Plus. Update payment method",
        
        # Offers and promotions
        "{platform} SALE! Up to {discount}% off on {category}. Shop now!",
        "Flash sale starting in {time} minutes on {platform}. Get ready!",
        "Exclusive deal for you! {item} at {discount}% off. Limited time!",
        "Weekend special: Free delivery on orders above Rs.{amount}",
        
        # Account and profile
        "Welcome to {platform}! Get {discount}% off on your first order",
        "Order history exported. Download from {platform} app",
        "Your {platform} profile updated successfully",
        "New address added to your {platform} account",
        "Password changed successfully for {platform} account",
        
        # Modern features
        "Your {item} order is ready for pickup at {platform} store",
        "Schedule your grocery delivery for tomorrow with {platform}",
        "Live tracking enabled for order #{order_id}",
        "Voice order placed successfully on {platform}. Rs.{amount} total",
        "{platform} Pay Later: Rs.{amount} due on {date}",
    ]
    
    platforms = [
        "Amazon", "Flipkart", "Myntra", "Ajio", "Nykaa", "BigBasket", "Grofers", "Blinkit",
        "Swiggy", "Zomato", "Dunzo", "Zepto", "FreshToHome",
        "Uber", "Ola", "Rapido", "Bounce", "Yulu",
        "BookMyShow", "PayTM", "PhonePe", "Cred", "Jupiter",
        "Meesho", "Urban Company", "Licious", "Country Delight"
    ]
    
    restaurants = ["McDonald's", "KFC", "Pizza Hut", "Domino's", "Subway", "Starbucks", "CCD", "Burger King"]
    items_ecom = ["Mobile", "Laptop", "Headphones", "Shoes", "Shirt", "Dress", "Books", "Groceries", "Medicine"]
    categories_ecom = ["Electronics", "Fashion", "Home", "Books", "Grocery", "Beauty", "Sports"]
    statuses = ["confirmed", "packed", "shipped", "out for delivery", "delivered"]
    
    for i in range(samples_per_category):
        template = random.choice(ecommerce_templates)
        data = {
            'platform': random.choice(platforms),
            'order_id': random.randint(10000000, 99999999),
            'time': random.choice(["10 PM", "2 PM", "30", "45", "60"]),
            'link': f"{random.choice(['amzn', 'fkrt', 'myntra'])}.to/track{random.randint(100,999)}",
            'date': f"{random.randint(15,30)}-{random.choice(['Jan', 'Feb', 'Mar'])}-25",
            'amount': random.randint(99, 15000),
            'restaurant': random.choice(restaurants),
            'code': f"CODE{random.randint(100,999)}",
            'discount': random.randint(10, 70),
            'item': random.choice(items_ecom),
            'category': random.choice(categories_ecom),
            'status': random.choice(statuses),
            'days': random.randint(1, 30)
        }
        
        try:
            msg = template.format(**data)
            training_data.append((msg, categories['ECOMMERCE']))
        except:
            continue
    
    # ============= EXPANDED INBOX MESSAGES =============
    logger.info("Generating INBOX messages...")
    
    inbox_templates = [
        # Personal messages
        "Hi, are you free for lunch today? Let me know!",
        "Meeting rescheduled to {time} tomorrow. Please confirm attendance",
        "Happy birthday! Hope you have a wonderful day ahead!",
        "Don't forget about the presentation at {time} today. Good luck!",
        "Thanks for your help with the project. Really appreciate it!",
        "Can you please send me the report by EOD? Thanks!",
        "Mom calling for dinner. Come home by {time}",
        "Happy anniversary! Best wishes for many more years together",
        "Congratulations on your promotion! Well deserved.",
        "How was your day? Call me when you're free",
        
        # Work/Office
        "Team meeting at {time} in conference room {room}",
        "Client presentation moved to {time}. Prepare accordingly",
        "Deadline for project submission is {date}. Please complete on time",
        "Your leave application has been approved for {date}",
        "Office party tomorrow at {time}. Please confirm attendance",
        "Interview scheduled for {time} on Monday. Good luck!",
        "Salary credited to your account. Check payslip on portal",
        "Annual appraisal meeting scheduled for {date}",
        "New project assigned. Check email for details",
        "Work from home approved for {date}",
        
        # Travel and bookings
        "Flight delay notification: Your flight {flight} is delayed by {time} hours",
        "Your PNR {pnr} for train journey on {date} is confirmed",
        "Hotel booking confirmed for {date}. Check in after 2 PM",
        "Cab booked for {time}. Driver will call you",
        "Your booking at {restaurant} for {time} is confirmed",
        
        # Appointments and reminders
        "Appointment reminder: Doctor visit tomorrow at {time}. Bring reports",
        "Your car service is due. Please book appointment",
        "Dental checkup scheduled for {date} at {time}",
        "Gym session booked for {time}. Don't miss it!",
        "Salon appointment confirmed for {date} at {time}",
        
        # Utilities and bills
        "Your electricity bill of Rs.{amount} is due on {date}",
        "Water supply will be disrupted tomorrow from {time} to {time2}",
        "Gas cylinder booking confirmed. Delivery by {date}",
        "Internet will be down for maintenance on {date}",
        "Your broadband plan expires on {date}. Renew soon",
        
        # Educational
        "Class cancelled today due to faculty unavailability",
        "Exam scheduled for {date}. Syllabus: {subject}",
        "Library books due for return by {date}. Avoid late fees",
        "Workshop on {subject} tomorrow. Don't forget to bring laptop",
        "Assignment submission deadline: {date}",
        "Parent-teacher meeting scheduled for {date} at {time}",
        "Results declared. Check student portal",
        
        # Social and events
        "Movie tickets booked for {time} show. See you at cinema",
        "Party at my place this weekend. You're invited!",
        "Wedding invitation for {date}. Save the date!",
        "Birthday party tomorrow at {time}. Bring gifts!",
        "Game night at {time}. Bring your A-game!",
        
        # Health and fitness
        "Your health checkup report is ready. Collect from clinic",
        "Vaccination reminder: Second dose due on {date}",
        "Your gym membership expires next week. Renew soon",
        "Medicine reminder: Take tablets after dinner",
        "Blood donation camp on {date}. Please participate",
        
        # Weather and alerts
        "Weather alert: Heavy rain expected. Carry umbrella",
        "Temperature dropping to {temp}°C. Stay warm!",
        "Cyclone warning issued. Stay indoors tomorrow",
        "Air quality poor today. Avoid outdoor activities",
        
        # General notifications
        "Your package has arrived. Please collect from reception",
        "Bank statement generated. Download from mobile app",
        "Insurance renewal due on {date}. Don't forget!",
        "Your subscription to {service} ends on {date}",
        "Reminder: Submit documents to HR by {date}",
        
        # Family and friends
        "Missing you! When are you visiting home?",
        "Kids want to meet uncle. Plan a visit soon",
        "Festival celebrations at home. You're expected!",
        "Your favorite show is starting. Switch to channel {channel}",
        "Made your favorite dish today. Come for dinner",
        
        # Modern lifestyle
        "Your food delivery is running late. ETA: {time}",
        "Parking slot {slot} booked for your visit",
        "Your cab is 2 minutes away. Please come downstairs",
        "WiFi password changed to: {password}",
        "Your phone is fully charged. You can unplug now",
    ]
    
    times = ["9 AM", "10 AM", "2 PM", "3 PM", "6 PM", "7 PM", "8 PM"]
    dates = [f"{random.randint(15,30)}-Jan-25", f"{random.randint(1,28)}-Feb-25", f"{random.randint(1,28)}-Mar-25"]
    subjects = ["Python", "Machine Learning", "Data Science", "Web Development", "AI", "Blockchain"]
    restaurants_local = ["Cafe Coffee Day", "McDonald's", "KFC", "Domino's", "Local Dhaba"]
    services_sub = ["Netflix", "Prime", "Spotify", "Hotstar", "YouTube Premium"]
    
    # Generate diverse inbox messages
    for i in range(samples_per_category):
        if i < len(inbox_templates):
            template = inbox_templates[i]
        else:
            template = random.choice(inbox_templates)
            
        data = {
            'time': random.choice(times),
            'time2': random.choice(times),
            'date': random.choice(dates),
            'amount': random.randint(500, 5000),
            'room': random.choice(["A", "B", "Conference", "Board"]) + str(random.randint(1,5)),
            'flight': f"AI{random.randint(100,999)}",
            'pnr': random.randint(1000000000, 9999999999),
            'restaurant': random.choice(restaurants_local),
            'subject': random.choice(subjects),
            'temp': random.randint(10, 45),
            'service': random.choice(services_sub),
            'channel': random.randint(1, 500),
            'slot': f"A{random.randint(1,50)}",
            'password': f"wifi{random.randint(1000,9999)}"
        }
        
        try:
            msg = template.format(**data)
            training_data.append((msg, categories['INBOX']))
        except:
            # Simple message as fallback
            simple_messages = [
                "Hi, how are you doing?",
                "Let's meet for coffee sometime",
                "Thanks for your help yesterday",
                "Don't forget our meeting today",
                "See you at the party tonight"
            ]
            msg = random.choice(simple_messages)
            training_data.append((msg, categories['INBOX']))
    
    # ============= EXPANDED NEEDS_REVIEW MESSAGES =============
    logger.info("Generating NEEDS_REVIEW messages...")
    
    review_templates = [
        # Account related
        "Please update your details to continue service",
        "Important notice regarding your account",
        "Action required for your profile verification",
        "Your request has been processed successfully",
        "Thank you for your recent transaction",
        "We have received your application",
        "Your booking is confirmed for today",
        "Please contact customer service for assistance",
        "Your subscription will expire soon",
        "New features available in your account",
        
        # Verification and documentation
        "Profile verification pending. Complete within {days} days",
        "Document submitted successfully. Processing will take {days} days",
        "Additional documents required for verification",
        "Your KYC verification is under review",
        "Identity verification completed successfully",
        "Address proof verification pending",
        "Income documents uploaded successfully",
        "Your application is under review",
        
        # System notifications
        "Your feedback has been recorded. Thank you",
        "Account security update completed successfully",
        "New policy update. Please review and accept",
        "Service maintenance scheduled for tomorrow {time}",
        "Your recent inquiry is under review",
        "Password changed successfully. If not done by you, contact support",
        "Account settings updated as per your request",
        "New terms and conditions effective from next month",
        "Privacy policy updated. Please review changes",
        "Your complaint has been registered. Ticket: {ticket}",
        
        # Processing and approvals
        "Your {request_type} request is being processed",
        "Please complete the {action} process",
        "System {action} scheduled for maintenance",
        "Thank you for the {action}. We'll get back soon",
        "Additional {action} may be required",
        "Your {service} upgrade is in progress",
        "Approval pending for your {request_type}",
        "Your {document} has been verified",
        
        # Service related
        "Service request #{ticket} created successfully",
        "Your issue has been escalated to senior team",
        "Resolution time: {days} business days",
        "Service will be restored by {time}",
        "Technical team is working on your issue",
        "Your {service} will be activated within {days} hours",
        "Installation scheduled for {date} between {time}-{time2}",
        "Service downtime expected on {date}",
        
        # Generic business
        "Message delivered successfully",
        "Your submission has been acknowledged",
        "Processing fee of Rs.{amount} is applicable",
        "Your query is important to us",
        "We value your association with us",
        "Thank you for choosing our services",
        "Your order is being prepared",
        "Delivery scheduled for {date}",
        "Service confirmation: REF{ref}",
        "Your appointment is confirmed",
        
        # Status updates
        "Status updated to: In Progress",
        "Your case has been assigned to {name}",
        "Follow up required on {date}",
        "Please provide additional information",
        "Your request is in queue",
        "Estimated completion time: {days} days",
        "Priority level: {priority}",
        "Current status: Under Review",
        
        # Government/official
        "Your application reference: {ref}",
        "Acknowledgment number: ACK{ref}",
        "Please visit office with original documents",
        "Your certificate will be ready in {days} days",
        "Fee payment confirmation received",
        "Your registration is successful",
        "Renewal reminder for {service}",
        "Compliance update required",
        
        # Modern services
        "OTP will be sent shortly",
        "Link activation in progress",
        "Your profile is {percent}% complete",
        "Verification email sent to your registered email",
        "SMS notifications enabled for your account",
        "Your preferences have been saved",
        "Account sync completed",
        "Backup created successfully",
        
        # Ambiguous service messages
        "Important update available",
        "Please check your registered email",
        "Your recent activity summary",
        "Monthly statement is ready",
        "Service usage report generated",
        "Your plan details have been updated",
        "Renewal options available",
        "Special offer exclusively for you",
        "Your loyalty points: {points}",
        "Account summary attached"
    ]
    
    request_types = ["loan", "card", "service", "upgrade", "cancellation", "refund", "transfer"]
    actions = ["verification", "update", "review", "confirmation", "approval"]
    services_review = ["internet", "mobile", "DTH", "gas", "electricity", "water", "insurance"]
    documents = ["Aadhaar", "PAN", "passport", "license", "certificate"]
    priorities = ["High", "Medium", "Normal", "Low"]
    
    for i in range(samples_per_category):
        if i < len(review_templates):
            template = review_templates[i]
        else:
            template = random.choice(review_templates)
            
        data = {
            'days': random.randint(1, 15),
            'time': random.choice(["2-4 AM", "10 AM", "2 PM", "6 PM"]),
            'time2': random.choice(["12 PM", "4 PM", "8 PM"]),
            'ticket': random.randint(100000, 999999),
            'request_type': random.choice(request_types),
            'action': random.choice(actions),
            'service': random.choice(services_review),
            'document': random.choice(documents),
            'date': random.choice(dates),
            'amount': random.randint(100, 2000),
            'name': random.choice(["Ravi", "Priya", "Support Team", "Manager"]),
            'ref': random.randint(10000, 99999),
            'priority': random.choice(priorities),
            'percent': random.randint(50, 95),
            'points': random.randint(100, 10000)
        }
        
        try:
            msg = template.format(**data)
            training_data.append((msg, categories['NEEDS_REVIEW']))
        except:
            # Simple fallback
            simple_review = [
                "Your request is being processed",
                "Thank you for contacting us",
                "Please check your email for updates",
                "Your application is under review",
                "Service will be activated soon"
            ]
            msg = random.choice(simple_review)
            training_data.append((msg, categories['NEEDS_REVIEW']))
    
    logger.info(f"Generated {len(training_data)} enhanced training samples")
    
    # Show distribution
    label_counts = {}
    for _, label in training_data:
        label_counts[label] = label_counts.get(label, 0) + 1
    
    logger.info("Enhanced label distribution:")
    for category, idx in categories.items():
        logger.info(f"  {category}: {label_counts.get(idx, 0)} samples")
    
    return training_data

def train_enhanced_synthetic_model():
    """Train model with enhanced synthetic data"""
    
    try:
        import tensorflow as tf
        from tensorflow.keras.models import Sequential
        from tensorflow.keras.layers import Dense, Embedding, GlobalAveragePooling1D, Dropout
        from tensorflow.keras.preprocessing.text import Tokenizer
        from tensorflow.keras.preprocessing.sequence import pad_sequences
        from sklearn.metrics import classification_report, confusion_matrix
        
        logger.info("Starting enhanced synthetic model training...")
        
        # Create enhanced training data
        training_data = create_enhanced_synthetic_data()
        
        # Separate texts and labels
        texts = [item[0] for item in training_data]
        labels = [item[1] for item in training_data]
        
        # Convert to DataFrame
        df = pd.DataFrame({'text': texts, 'label': labels})
        logger.info(f"Enhanced dataset shape: {df.shape}")
        
        # Split data with stratification
        X_train, X_test, y_train, y_test = train_test_split(
            df['text'], df['label'], test_size=0.2, random_state=42, stratify=df['label']
        )
        
        # Tokenization with larger vocabulary for more patterns
        logger.info("Tokenizing enhanced text...")
        max_words = 7000  # Increased vocabulary
        max_len = 80      # Longer sequences for complex messages
        
        tokenizer = Tokenizer(num_words=max_words, oov_token='<OOV>')
        tokenizer.fit_on_texts(X_train)
        
        # Convert text to sequences
        X_train_seq = tokenizer.texts_to_sequences(X_train)
        X_test_seq = tokenizer.texts_to_sequences(X_test)
        
        # Pad sequences
        X_train_pad = pad_sequences(X_train_seq, maxlen=max_len, padding='post', truncating='post')
        X_test_pad = pad_sequences(X_test_seq, maxlen=max_len, padding='post', truncating='post')
        
        logger.info(f"Enhanced training shape: {X_train_pad.shape}")
        logger.info(f"Enhanced test shape: {X_test_pad.shape}")
        
        # Calculate class weights
        class_weights = compute_class_weight(
            'balanced', 
            classes=np.unique(y_train), 
            y=y_train
        )
        class_weight_dict = dict(zip(np.unique(y_train), class_weights))
        logger.info(f"Enhanced class weights: {class_weight_dict}")
        
        # Build enhanced model architecture
        logger.info("Building enhanced model architecture...")
        
        model = Sequential([
            Embedding(max_words, 128),  # Larger embedding dimension
            GlobalAveragePooling1D(),
            Dense(256, activation='relu'),  # Larger first layer
            Dropout(0.5),
            Dense(128, activation='relu'),  # Larger second layer  
            Dropout(0.4),
            Dense(64, activation='relu'),
            Dropout(0.3),
            Dense(32, activation='relu'),
            Dropout(0.2),
            Dense(6, activation='softmax')
        ])
        
        model.compile(
            optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy']
        )
        
        logger.info("Enhanced model architecture:")
        model.summary()
        
        # Advanced training with callbacks
        logger.info("Training enhanced model...")
        from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau, ModelCheckpoint
        
        callbacks = [
            EarlyStopping(
                monitor='val_accuracy',
                patience=15,  # More patience for larger dataset
                restore_best_weights=True,
                verbose=1
            ),
            ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=7,
                min_lr=0.00001,
                verbose=1
            ),
            ModelCheckpoint(
                'models/best_enhanced_model.keras',
                monitor='val_accuracy',
                save_best_only=True,
                verbose=1
            )
        ]
        
        history = model.fit(
            X_train_pad, y_train,
            batch_size=64,  # Larger batch size for efficiency
            epochs=50,  # More epochs for larger dataset
            validation_split=0.2,
            class_weight=class_weight_dict,
            callbacks=callbacks,
            verbose=1
        )
        
        # Evaluate model
        logger.info("Evaluating enhanced model...")
        test_loss, test_acc = model.evaluate(X_test_pad, y_test, verbose=0)
        logger.info(f"Enhanced test accuracy: {test_acc:.4f}")
        
        # Detailed predictions analysis
        y_pred = model.predict(X_test_pad, verbose=0)
        y_pred_classes = np.argmax(y_pred, axis=1)
        
        # Classification report
        category_names = ['INBOX', 'SPAM', 'OTP', 'BANKING', 'ECOMMERCE', 'NEEDS_REVIEW']
        print("\nEnhanced Classification Report:")
        print(classification_report(y_test, y_pred_classes, target_names=category_names, zero_division=0))
        
        # Confusion matrix
        cm = confusion_matrix(y_test, y_pred_classes)
        logger.info("Enhanced Confusion Matrix:")
        logger.info(f"Categories: {category_names}")
        for i, row in enumerate(cm):
            logger.info(f"{category_names[i]}: {row}")
        
        # Save model
        models_dir = Path("models")
        models_dir.mkdir(exist_ok=True)
        
        model_path = models_dir / "enhanced_synthetic_classifier.keras"
        model.save(str(model_path))
        logger.info(f"Enhanced model saved to {model_path}")
        
        # Save tokenizer
        import pickle
        tokenizer_path = models_dir / "enhanced_synthetic_tokenizer.pkl"
        with open(tokenizer_path, 'wb') as f:
            pickle.dump(tokenizer, f)
        logger.info(f"Enhanced tokenizer saved to {tokenizer_path}")
        
        # Convert to TensorFlow Lite
        logger.info("Converting enhanced model to TensorFlow Lite...")
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS,
            tf.lite.OpsSet.SELECT_TF_OPS
        ]
        converter.target_spec.supported_types = [tf.float32]
        
        tflite_model = converter.convert()
        
        tflite_path = models_dir / "enhanced_synthetic_classifier.tflite"
        with open(tflite_path, 'wb') as f:
            f.write(tflite_model)
        
        size_mb = len(tflite_model) / (1024 * 1024)
        logger.info(f"Enhanced TFLite model saved to {tflite_path}")
        logger.info(f"Enhanced model size: {size_mb:.2f}MB")
        
        # Test TFLite model
        logger.info("Testing enhanced TensorFlow Lite model...")
        interpreter = tf.lite.Interpreter(model_content=tflite_model)
        interpreter.allocate_tensors()
        
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        # Comprehensive test samples
        test_samples = [
            ("Your OTP is 456789. Valid for 10 minutes. Do not share.", "OTP"),
            ("🎉 CONGRATULATIONS! You've won Rs.100000! Call 9876543210 NOW!", "SPAM"),
            ("Hi, are you free for lunch today? Let me know!", "INBOX"),
            ("Rs.15000 debited from A/c XX1234 on 15-JAN-25. Balance: Rs.45000", "BANKING"),
            ("Your Amazon order #12345678 is out for delivery. Expected by 6 PM.", "ECOMMERCE"),
            ("Please update your profile details to continue service.", "NEEDS_REVIEW"),
            ("URGENT: Double your money in 30 days! Invest Rs.10000 get Rs.20000", "SPAM"),
            ("Low balance alert: Your HDFC A/c XX1234 balance is Rs.500", "BANKING"),
            ("Hi dude, movie tonight at 8 PM? Let me know ASAP!", "INBOX"),
            ("Your Swiggy order from McDonald's will arrive in 25 minutes", "ECOMMERCE")
        ]
        
        logger.info("Enhanced sample predictions:")
        correct_predictions = 0
        for text, expected in test_samples:
            # Preprocess
            sample_seq = tokenizer.texts_to_sequences([text])
            sample_pad = pad_sequences(sample_seq, maxlen=max_len, padding='post', truncating='post')
            
            # Resize interpreter
            input_shape = sample_pad.shape
            interpreter.resize_tensor_input(input_details[0]['index'], input_shape)
            interpreter.allocate_tensors()
            
            # Predict
            interpreter.set_tensor(input_details[0]['index'], sample_pad.astype(np.float32))
            interpreter.invoke()
            
            output_data = interpreter.get_tensor(output_details[0]['index'])
            predicted_class = np.argmax(output_data[0])
            confidence = np.max(output_data[0])
            predicted_category = category_names[predicted_class]
            
            is_correct = predicted_category == expected
            if is_correct:
                correct_predictions += 1
            
            logger.info(f"  Text: {text[:50]}...")
            logger.info(f"  Expected: {expected}, Predicted: {predicted_category} ({confidence:.3f}) {'✓' if is_correct else '✗'}")
        
        sample_accuracy = correct_predictions / len(test_samples)
        logger.info(f"Enhanced sample test accuracy: {sample_accuracy:.1%}")
        
        # Create comprehensive metadata
        metadata = {
            'model_type': 'Enhanced Synthetic Dense Network',
            'categories': category_names,
            'max_words': max_words,
            'max_len': max_len,
            'test_accuracy': float(test_acc),
            'sample_test_accuracy': float(sample_accuracy),
            'model_size_mb': float(size_mb),
            'training_samples': len(training_data),
            'samples_per_category': len(training_data) // 6,
            'architecture': 'Embedding(128) + GlobalAveragePooling1D + 4x Dense layers (256,128,64,32)',
            'features': [
                'enhanced_synthetic_data',
                'larger_vocabulary_7000',
                'longer_sequences_80',
                'larger_architecture',
                'balanced_dataset',
                'class_weights', 
                'early_stopping',
                'learning_rate_scheduling',
                'model_checkpointing',
                'dynamic_batch_size'
            ],
            'training_improvements': [
                '200_samples_per_category',
                'diverse_message_patterns',
                'modern_app_notifications',
                'regional_language_hints',
                'edge_cases_coverage',
                'ambiguous_message_handling'
            ],
            'class_weights': {str(k): float(v) for k, v in class_weight_dict.items()}
        }
        
        import json
        metadata_path = models_dir / "enhanced_synthetic_metadata.json"
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)
        logger.info(f"Enhanced metadata saved to {metadata_path}")
        
        logger.info("🎉 Enhanced synthetic training completed successfully!")
        logger.info(f"📱 Mobile-ready enhanced model: {size_mb:.2f}MB")
        logger.info(f"🎯 Test accuracy: {test_acc:.1%}")
        logger.info(f"🎯 Sample test accuracy: {sample_accuracy:.1%}")
        
        return {
            'model_path': str(model_path),
            'tflite_path': str(tflite_path),
            'tokenizer_path': str(tokenizer_path),
            'test_accuracy': test_acc,
            'sample_accuracy': sample_accuracy,
            'model_size_mb': size_mb
        }
        
    except Exception as e:
        logger.error(f"Enhanced training failed: {e}")
        import traceback
        traceback.print_exc()
        return None

def main():
    """Main enhanced synthetic training function"""
    logger.info("🚀 Enhanced Synthetic SMS Classifier Training")
    logger.info("=" * 70)
    
    # Train enhanced model
    result = train_enhanced_synthetic_model()
    
    if result:
        print("\n" + "="*70)
        print("✅ SUCCESS: Enhanced Synthetic Mobile SMS Classifier Trained!")
        print("="*70)
        print(f"📁 Model files:")
        print(f"  • Keras model: {result['model_path']}")
        print(f"  • TFLite model: {result['tflite_path']} ({result['model_size_mb']:.2f}MB)")
        print(f"  • Tokenizer: {result['tokenizer_path']}")
        print(f"🎯 Test accuracy: {result['test_accuracy']:.1%}")
        print(f"🎯 Sample test accuracy: {result['sample_accuracy']:.1%}")
        print(f"📱 Ready for Android integration!")
        print("="*70)
        print(f"🔧 Next steps:")
        print(f"  1. Copy {Path(result['tflite_path']).name} to android/app/src/main/assets/")
        print(f"  2. Update Android integration to use enhanced model")
        print(f"  3. Test with diverse real SMS messages")
        print("="*70)
        print("📊 Enhanced improvements made:")
        print("  • Expanded to 200 samples per category (1200 total)")
        print("  • More diverse and realistic message patterns")
        print("  • Modern app notifications and fintech messages")
        print("  • Regional language hints and edge cases")
        print("  • Larger model architecture (256→128→64→32)")
        print("  • Increased vocabulary size (7000 words)")
        print("  • Longer sequence length (80 tokens)")
        print("  • Advanced training callbacks and monitoring")
        print("="*70)
    else:
        print("\n❌ Enhanced training failed. Please check the logs above.")

if __name__ == "__main__":
    main()