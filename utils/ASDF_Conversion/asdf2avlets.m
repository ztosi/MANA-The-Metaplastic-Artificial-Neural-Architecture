%
% Utilizes a depth-first search to determines causal webs, their sizes, and
% spontaneous events.
%
% Inputs: asdf, sig_del (raw 1-ms-bin asdf, and TE delays matrix)
% 
% Requires functions: asdfCast, ASDFToSparse
%
% Rashid Williams-Garcia 12/10/15

function [cShapes,cSizes,spontEvents] = asdf2avlets(asdf,sig_del)
    asdf = asdfCast(asdf);
    [raster,~] = ASDFToSparse(asdf);

    %% Main code
    N = length(asdf)-2;
    NT = asdf{end}(2);

    if NT<2^8
        p = 'uint8';
    elseif NT<2^16
        p = 'uint16';
    elseif NT<2^32
        p = 'uint32';
    else
        p = 'double';
    end

    tic
    %determine descendants    
    descmat = cell(N,1);
    for i=1:N
        descmat{i} = find(sig_del(i,:));
    end    
    
    dSD = zeros(size(sig_del));
    dSD(sig_del~=0)=2; %std. dev. of delay values (final version should have this as an input)
    maxDel = max(max(sig_del));
    
    av = 0; %avalanche counter
    cShapes = cell(1,1);
    %Lengths = zeros(1);
    %Spans = zeros(1);
    cSizes = zeros(1);
    spontEvents = cell(N,1);

    %find times with activity (active times)
    aTimes = find(any(raster,1)~=0)';%cast(find(any(raster,1)~=0)',p);
    while numel(aTimes)~=0
        dA = diff(aTimes);

        %identify macro-avalanche start and end times:
        t0 = min(aTimes);
        tf = aTimes(find(dA>maxDel,1));
        
        if isempty(tf)  %this happens if the recording ends mid-activity
            tf = max(aTimes);
        end
        %t = aTimes(1:find(dA>maxDel,1));

        [sites,times] = find(raster(:,t0:tf));
        times = times+t0-1;
        activations = horzcat(sites,times);
%         clear sites times
        nA = size(activations,1);

        associations = zeros(nA,1);    %contains activation component associations
        doublets = zeros(1,2,p);   %couples of causally-related activations
        x = 1;  %doublet counter

        for a=1:nA
            if associations(a)==0
                associations(a) = a;
            end

            n = activations(a,1);
            t0 = activations(a,2);

            %possible descendants of n and their delays
%             PDs = find(sig_del(n,:));
            PDs = descmat{n};

            if isempty(PDs)	%n doesn't have any outgoing connections!
                continue
            else
                dat = t0+sig_del(n,PDs); %mean possible descendant activation times
                lb = dat-dSD(n,PDs);
                lb(lb<=t0) = t0+1;
                ub = dat+dSD(n,PDs);

                for j=1:numel(PDs)
                    if ub(j)>NT
                        dt = lb(j):NT;
                    else
                        dt = lb(j):ub(j);
                    end
                    temp = find(raster(PDs(j),dt),1);

                    if isempty(temp)    %n doesn't branch its activity!
                        continue
                    else
                        %this is about an order of mag. faster than ismember method...
                        b = find(activations(:,1)==PDs(j) & activations(:,2)==dt(temp));
                        doublets(x,:) = [a,b];
                        x = x+1;

                        if associations(b)==0
                            associations(b) = associations(a);
                        elseif associations(b)~=b
                            c = min(associations(a),associations(b));
                            associations(associations==associations(a)) = c;
                            associations(associations==associations(b)) = c;
                        elseif associations(b)==b
                            error('How could you let this happen?!')
                        end
                    end
                end
            end
        end
        
        avIDs = unique(associations);
        Nav = numel(avIDs);

        for y=1:Nav
            av = av+1;
            %find activations which are members of the avalanche:
            temp = find(associations==avIDs(y));    %activation indeces
            avSize = numel(temp);
            n = activations(temp,1);    %neuron IDs
            t0 = activations(temp,2);   %timesteps
            
            for i=1:avSize
                [~,col] = find(doublets==temp(i));
                %col is awesome! Numbers of 1's indicate numbers of
                %descendants of the activation temp(i); numbers of 2's
                %indicate numbers of ancestors!

                %Pick out spontaneous events:
                if sum(unique(col))==1 || isempty(col)
                    spontEvents{n(i)} = [spontEvents{n(i)} cast(t0(i),p)];
                end
            end

            cShapes{av,1} = vertcat(cast([n t0],p));
            %Lengths(av,1) = numel(unique(t0));
            %Spans(av,1) = max(t0)-min(t0)+1;
            cSizes(av,1) = avSize;
        end
        
        aTimes = aTimes(aTimes>tf);
    end
end