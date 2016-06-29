%
% Optimizes asdf data precision to minimize disk usage.
%
% Rashid Williams-Garcia 12/11/15

function asdfNeu = asdfCast(asdf)
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
    
    asdfNeu = cell(size(asdf));
    
    for i=1:length(asdf)-2
        asdfNeu{i} = cast(asdf{i},p);
    end
    
    asdfNeu{end-1} = asdf{end-1};
    asdfNeu{end} = asdf{end};
end