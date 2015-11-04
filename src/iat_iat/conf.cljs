(ns iat-iat.conf)

(def conf
    {:keys {
      :instruction 32
      :left 69
      :right 73
      }
     :times {
      :label 1000
      :cross 500
      :transition 250
      }
     :factors {
      "personnel"
      {:random true
       :color "#4F5"
       :categories {
        "moi" #{"je"}
        "autrui" #{"eux"}
        }
      }
      "pouvoir"
      {:random false
       :color "#FFF"
       :categories {
        "dominance" #{"fort"}
        "soumission" #{"faible"}
       }
      }
    }
   :intro "Merci de nous accorder un peu de votre temps."
   :end "Et voilà, c'est fini."
   :blocks
    [
      {:instruction "3. Appuyer sur e et i le plus rapidement possible"
       :label "Personnel et pouvoir"
       :factors ["personnel" "pouvoir"]
      }
      {:instruction "1. Appuyer sur e et i le plus rapidement possible"
       :label "personnel"
       :factors ["personnel"]
      }
      {:instruction "2. Appuyer sur e et i le plus rapidement possible"
       :label "pouvoir"
       :factors ["pouvoir"]
      }
    ]
  })