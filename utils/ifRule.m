function [v, w, s] = ifRule(t, dt, curr)

    v = zeros(1,length(t)+1);
    w = zeros(1,length(t)+1);
    s = 0;
    v(1) = -70;
    pause = 1;
    t_spk = 0;
    for i =2:length(t)+1
        if t(i-1)-t_spk > 2
            pause=1;
        end
        
        v(i) = v(i-1) + (pause * dt*((-70-v(i-1)) +  curr(i-1) - w(i-1))/23);
        %w(i) = w(i-1) + dt*((v(i-1)+70) - w(i-1))/144; 
        w(i) = w(i-1) + dt*(-w(i-1))/144; 
        if v(i) > -50
            v(i) = -55;
            w(i) = w(i) + 20;
            s = s+1;
            t_spk = t(i-1);
            pause = 0;
        end
    end
nuP = (exp(-nuP/(beta*v0))*(thresh-ThA)* (thresh>ThA)) + ((thresh<ThA)*( ( ( (thresh-ThA)*nuP/v0)*(nuP<=v0) ) + ( (thresh-ThA)*((1+(log(1+alpha*(nuP/v0 - 1)))/alpha)) *(nuP>v0) ) )/eta; 

end